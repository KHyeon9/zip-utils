package io.github.khyeon9.ziputils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZIP 파일을 지정한 디렉터리에 압축 해제하는 유틸리티 클래스.
 *
 * <p>
 * ZIP Entry 이름은 UTF-8 또는 지정한 Charset으로 처리하며,
 * Zip Slip(Path Traversal) 공격을 방지한다.
 * </p>
 */
public final class ZipExtractor {
    // 인스턴스 생성 방지
    private ZipExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }
    // 기본 ZIP Entry 문자셋
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 기본 문자셋(UTF-8) 및 기존 파일 덮어쓰기 모드로 ZIP 파일을 압축 해제합니다.
     */
    public static int extract(Path zipFile, Path targetPath) throws IOException {
        return extract(zipFile, targetPath, DEFAULT_CHARSET, true);
    }

    /**
     * 지정한 문자셋 및 기존 파일 덮어쓰기 모드로 ZIP 파일을 압축 해제합니다.
     */
    public static int extract(Path zipFile, Path targetPath, Charset charset) throws IOException {
        return extract(zipFile, targetPath, charset, true);
    }

    /**
     * ZIP 파일을 지정한 폴더 위치에 압축 해제합니다.
     *
     * @param zipFile    ZIP 파일 경로
     * @param targetPath 압축을 풀 대상 디렉터리 경로
     * @param charset    ZIP 엔트리 이름을 해석할 문자셋
     * @param overwrite  true이면 기존 파일을 덮어쓰고, false이면 기존 파일이 존재할 경우 건너뜀
     * @return 압축 해제된 파일의 총 개수 (디렉터리 제외)
     * @throws NoSuchFileException 원본 ZIP 파일이 존재하지 않거나 정규 파일이 아닌 경우
     * @throws IOException         압축 해제 중 입출력 오류가 발생하거나 Zip Slip이 감지된 경우
     */
    public static int extract(Path zipFile, Path targetPath, Charset charset, boolean overwrite) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile이 null입니다.");
        Objects.requireNonNull(targetPath, "targetPath가 null입니다.");
        Objects.requireNonNull(charset, "charset이 null입니다.");

        if (!Files.isRegularFile(zipFile)) {
            throw new NoSuchFileException(zipFile.toString());
        }

        Files.createDirectories(targetPath);

        // ZIP 입력 스트림 생성
        try (
                InputStream inputStream = Files.newInputStream(zipFile);
                ZipInputStream zipInputStream = new ZipInputStream(inputStream, charset)
        ){
            int extracted = 0;
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                try {
                    Path target = resolveEntry(targetPath, zipEntry);
                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(target);
                        continue;
                    }

                    Files.createDirectories(target.getParent());
                    // 덮어쓰기 가능한 유무에 따른 구분
                    if (overwrite) {
                        Files.copy(
                                zipInputStream,
                                target,
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    } else {
                        if (Files.exists(target)) {
                            continue;
                        }
                        Files.copy(zipInputStream, target);
                    }
                    extracted++;
                } finally {
                    zipInputStream.closeEntry();
                }
            }
            return extracted;
        }
    }

    /**
     * ZIP Entry의 안전한 저장 경로를 생성하고 Zip Slip 공격 여부를 검사합니다.
     */
    private static Path resolveEntry(Path targetPath, ZipEntry zipEntry) throws IOException {
        Path normalizedRoot = targetPath.toAbsolutePath().normalize();
        Path normalizedTarget = targetPath.resolve(zipEntry.getName()).toAbsolutePath().normalize();

        // 상위 디렉터리 탈출 공격(Zip Slip) 검증
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new IOException("Zip Slip 문제 감지 : " + zipEntry.getName());
        }
        return normalizedTarget;
    }
}