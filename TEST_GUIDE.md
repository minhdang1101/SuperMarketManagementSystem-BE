# Huong dan chay test Backend

File nay dung cho project `SuperMarketManagementSystem-BE`.

## 1. Kiem tra truoc khi test

Mo terminal tai thu muc backend:

```powershell
cd C:\Users\PC\Desktop\SuperMarketManagementSystem-BE
```

Kiem tra Java:

```powershell
java -version
javac -version
```

Project hien dang cau hinh:

```xml
<java.version>17</java.version>
```

Vi vay may nen dung JDK 17. Neu `pom.xml` bi doi ve Java 21 thi may can cai JDK 21 hoac doi lai `java.version` ve `17`.

## 2. Chay toan bo test

Dung Maven Wrapper co san trong project:

```powershell
.\mvnw.cmd test
```

Lenh nay se:

- Compile source code.
- Compile test code.
- Chay cac test trong `src/test/java`.
- Tao report Jacoco neu cau hinh plugin dang bat.

## 3. Chay compile nhanh, khong chay test

Dung khi chi muon kiem tra code co build duoc khong:

```powershell
.\mvnw.cmd -DskipTests compile
```

## 4. Chay mot test class cu the

Vi du chay test context hien co:

```powershell
.\mvnw.cmd -Dtest=SuperMarketManagementSystemApplicationTests test
```

## 5. Cau hinh test database

Test co file cau hinh rieng:

```text
src/test/resources/application.properties
```

File nay dang dung H2 in-memory:

```properties
spring.datasource.url=jdbc:h2:mem:smms_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
```

Muc dich la de test khong phu thuoc SQL Server local.

Neu gap loi:

```text
Cannot load driver class: org.h2.Driver
```

thi them dependency H2 vao `pom.xml`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

## 6. Loi thuong gap

### Loi Java version

Neu thay:

```text
release version 21 not supported
```

nghia la `pom.xml` dang yeu cau Java 21 nhung may dang dung JDK thap hon. Cach sua:

- Cai JDK 21 va tro `JAVA_HOME` ve JDK 21, hoac
- Doi `<java.version>` trong `pom.xml` ve version may dang co, vi du `17`.

### Loi ket noi database SQL Server

Neu test bi loi ket noi SQL Server, kha nang cao test dang doc nham `src/main/resources/application.properties`.

Can dam bao co file:

```text
src/test/resources/application.properties
```

de test dung H2 in-memory.

### Loi secret/mail/VNPay

Test khong nen can secret that. Cac bien nhu `JWT_SECRET`, `MAIL_PASSWORD`, `VNPAY_HASH_SECRET` nen de qua bien moi truong hoac gia tri default local, khong commit secret that len Git.

## 7. Lenh nen chay truoc khi push

```powershell
.\mvnw.cmd test
git status --short
```

Neu test pass va chi co file minh chu dong sua thi moi commit/push.
