# zip-utils
Java 기반 ZIP 압축 및 압축 해제 유틸리티 라이브러리입니다.

ZIP 파일 처리 시 필요한 Charset 지정, 기존 파일 덮어쓰기 제어를 지원하며,
압축과 압축 해제 과정에서 발생할 수 있는 Zip Slip(Path Traversal) 공격을 방지합니다.

## 요구 사항

- Java 11+

## 기능

- ZIP 파일 압축
- ZIP 파일 압축 해제
- UTF-8 기본 지원
- 사용자 지정 Charset 지원
- 기존 파일 덮어쓰기 제어
- Zip Slip(Path Traversal) 공격 방지
- 압축 및 압축 해제 파일 개수 반환

## 사용 방법

### ZIP 압축 해제

#### 기본 사용
```java
Path zipFile = Path.of("sample.zip");
Path targetPath = Path.of("output");

int count = ZipExtractor.extract(zipFile, targetPath);
```
### ZIP 압축
#### 기본 사용
```java
Path sourcePath = Path.of("data");
Path zipFile = Path.of("data.zip");

int count = ZipCompressor.compress(sourcePath, zipFile);
```
### 옵션
#### Charset 지정
```java
ZipExtractor.extract(
    zipFile,
    targetPath,
    Charset.forName("MS949")
);

ZipCompressor.compress(
    sourcePath,
    zipFile,
    Charset.forName("MS949")
);
```
#### 덮어쓰기 비활성화
```java
ZipExtractor.extract(
    zipFile,
    targetPath,
    StandardCharsets.UTF_8,
    false
);

ZipCompressor.compress(
    sourcePath,
    zipFile,
    StandardCharsets.UTF_8,
    false
);
```
## 제공 API

| 메서드 | 설명                     |
|--------|------------------------|
| `extract(Path, Path)` | UTF-8로 압축 해제           |
| `extract(Path, Path, Charset)` | Charset 지정 압축 해제       |
| `extract(Path, Path, Charset, boolean)` | Charset 및 Overwrite 설정 |
| `compress(Path, Path)` | UTF-8로 압축              |
| `compress(Path, Path, Charset)` | Charset 지정 압축          |
| `compress(Path, Path, Charset, boolean)` | Charset 및 Overwrite 설정 |

## 예외
IOException 또는 IllegalArgumentException이 발생할 수 있습니다.
- ZIP 파일이 존재하지 않는 경우
- 압축 또는 압축 해제 중 입출력 오류가 발생한 경우
- Zip Slip 공격이 감지된 경우
- 잘못된 ZIP Entry가 포함된 경우

### Zip Slip 방어
압축 해제 시 ZIP Entry 경로를 검증하여 Path Traversal 공격을 방지합니다.
```
../../../../etc/passwd
```
예를 들어서 위와 같은 ZIP Entry 경로가 포함된 경우 안전하지 않은 경로로 판단하여 압축 해제를 중단합니다.
