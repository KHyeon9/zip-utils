# zip-utils
ZIP 압축 해제 라이브러리입니다.

ZIP 파일을 지정한 경로에 안전하게 압축 해제하며, 문자셋(Charset) 지정과 덮어쓰기 옵션을 제공합니다.
또한 Zip Slip(Path Traversal) 공격을 방지하여 안전하게 사용할 수 있습니다.

## 개발 환경

- Java 11 이상

## 현재 기능

- ZIP 압축 해제
- UTF-8 기본 지원
- 사용자 지정 Charset 지원
- 기존 파일 덮어쓰기 여부 선택
- Zip Slip(Path Traversal) 공격 방지
- 압축 해제된 파일 개수 반환

## 사용 방법

### 기본 사용

```java
Path zipFile = Path.of("sample.zip");
Path targetPath = Path.of("output");

int count = ZipExtractor.extract(zipFile, targetPath);
```

### Charset 지정

```java
ZipExtractor.extract(
    zipFile,
    targetPath,
    Charset.forName("MS949")
);
```

### 덮어쓰기 비활성화

```java
ZipExtractor.extract(
    zipFile,
    targetPath,
    StandardCharsets.UTF_8,
    false
);
```

## 제공 API

| 메서드 | 설명 |
|--------|------|
| `extract(Path, Path)` | UTF-8로 압축 해제 |
| `extract(Path, Path, Charset)` | Charset 지정 |
| `extract(Path, Path, Charset, boolean)` | Charset 및 덮어쓰기 여부 지정 |

## 예외

다음과 같은 경우 `IOException`이 발생할 수 있습니다.

- ZIP 파일이 존재하지 않는 경우
- 압축 해제 중 입출력 오류가 발생한 경우
- Zip Slip 공격이 감지된 경우
