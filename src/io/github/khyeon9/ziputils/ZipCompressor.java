package io.github.khyeon9.ziputils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 파일 또는 디렉터리를 ZIP 파일로 압축하는 유틸리티 클래스.
 *
 * <p>
 * ZIP Entry 이름은 UTF-8 또는 지정한 Charset으로 저장하며,
 * 빈 디렉터리도 ZIP에 포함합니다.
 * </p>
 */
public final class ZipCompressor {
    // 인스턴스 생성 방지
    private ZipCompressor() {
        throw new UnsupportedOperationException("Utility class");
    }
    // 기본 ZIP Entry 문자셋
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * UTF-8 문자셋을 사용하여 파일 또는 디렉터리를 ZIP 파일로 압축합니다.
     *
     * <p>기존 ZIP 파일이 존재하면 덮어씁니다.
     */
    public static int compress(Path sourcePath, Path zipFile) throws IOException {
        return compress(sourcePath, zipFile, DEFAULT_CHARSET, true);
    }

    /**
     * 지정한 문자셋을 사용하여 파일 또는 디렉터리를 ZIP 파일로 압축합니다.
     *
     * <p>기존 ZIP 파일이 존재하면 덮어씁니다.
     */
    public static int compress(Path sourcePath, Path zipFile, Charset charset) throws IOException {
        return compress(sourcePath, zipFile, charset, true);
    }

    /**
     * 파일 또는 디렉터리를 ZIP 파일로 압축합니다.
     *
     * @param sourcePath 압축할 파일 또는 디렉터리 경로
     * @param zipFile 생성할 ZIP 파일 경로
     * @param charset ZIP Entry 이름을 저장할 문자셋
     * @param overwrite 기존 파일이 존재할 경우 덮어쓸지 여부
     * @return 압축된 파일의 총 개수(디렉터리 제외)
     * @throws IOException 압축 중 입출력 오류가 발생한 경우
     */
    public static int compress(Path sourcePath, Path zipFile, Charset charset, boolean overwrite) throws IOException {
        Objects.requireNonNull(sourcePath, "sourcePath가 null입니다.");
        Objects.requireNonNull(zipFile, "zipFile이 null입니다.");
        Objects.requireNonNull(charset, "charset이 null입니다.");
        // 압축할 파일이 없는 경우 처리
        if (!Files.exists(sourcePath)) {
            throw new NoSuchFileException(sourcePath.toString());
        }
        // 파일 또는 디렉터리가 아닌 경우 처리
        if (!Files.isRegularFile(sourcePath) && !Files.isDirectory(sourcePath)) {
            throw new IllegalArgumentException("파일 또는 디렉터리가 아닙니다: " + sourcePath);
        }
        // 덮어쓰기 허용이 되지 않지만 파일이 존재하는 경우 처리
        if (!overwrite && Files.exists(zipFile)) {
            throw new FileAlreadyExistsException(zipFile.toString());
        }
        // 부모 폴더가 없는 경우 생성
        Path parent = zipFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        int compressed = 0;
        // overwrite에 따라 옵션 처리
        OpenOption[] options = overwrite
                ? new OpenOption[] {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}
                : new OpenOption[] {StandardOpenOption.CREATE};

        // 파일 하나인 경우
        if (Files.isRegularFile(sourcePath)) {
            try (
                    OutputStream outputStream = Files.newOutputStream(zipFile, options);
                    ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, charset)
            ) {
                addFileEntry(zipOutputStream, sourcePath);
                return 1;
            }
        }
        // 디렉터리인 경우
        try (
                OutputStream outputStream = Files.newOutputStream(zipFile, options);
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, charset);
                Stream<Path> paths = Files.walk(sourcePath);
        ) {
            Iterator<Path> it = paths.iterator();

            while (it.hasNext()) {
                Path path = it.next();

                if (path.equals(sourcePath)) {
                    continue;
                }
                // 폴더의 경우도 압축에 포함
                if (Files.isDirectory(path)) {
                    ZipEntry entry = new ZipEntry(toEntryName(sourcePath, path) + "/");
                    zipOutputStream.putNextEntry(entry);
                    zipOutputStream.closeEntry();
                    continue;
                }

                addDirectoryFileEntry(zipOutputStream, sourcePath, path);
                compressed++;
            }

            return compressed;
        }
    }

    /**
     * 단일 파일을 ZIP Entry로 추가합니다.
     *
     * @param zipOutputStream ZIP 출력 스트림
     * @param file 압축할 파일
     * @throws IOException ZIP 기록 중 오류가 발생한 경우
     */
    private static void addFileEntry(
            ZipOutputStream zipOutputStream,
            Path file
    ) throws IOException {

        ZipEntry entry = new ZipEntry(file.getFileName().toString());
        zipOutputStream.putNextEntry(entry);

        try {
            Files.copy(file, zipOutputStream);
        } finally {
            zipOutputStream.closeEntry();
        }
    }

    /**
     * ZIP Entry를 생성하여 ZIP 출력 스트림에 추가합니다.
     *
     * @param zipOutputStream ZIP 출력 스트림
     * @param root 압축 기준 디렉터리
     * @param path 압축할 파일 경로
     * @throws IOException ZIP 기록 중 오류가 발생한 경우
     */
    private static void addDirectoryFileEntry(ZipOutputStream zipOutputStream, Path root, Path path) throws IOException {
        String entryName = toEntryName(root, path);

        ZipEntry entry = new ZipEntry(entryName);
        zipOutputStream.putNextEntry(entry);

        try {
            Files.copy(path, zipOutputStream);
        }  finally {
            zipOutputStream.closeEntry();
        }
    }

    /**
     * ZIP Entry 이름을 생성합니다.
     *
     * @param root 압축 기준 디렉터리
     * @param path 압축할 파일 경로
     * @return ZIP Entry 이름
     * @throws IllegalArgumentException 잘못된 상대 경로인 경우
     */
    private static String toEntryName(Path root, Path path) {
        Path relative = root.normalize().relativize(path.normalize());
        String entryName = relative.toString().replace('\\', '/');

        if (entryName.startsWith("../") || entryName.contains("/../")) {
            throw new IllegalArgumentException("잘못된 경로: " + entryName);
        }
        
        return entryName;
    }
}
