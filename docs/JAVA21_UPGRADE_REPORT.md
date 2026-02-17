# Java 21 Upgrade Report - BroadleafCommerce 4.0.x

## Baseline

| Item | Value |
|------|-------|
| Starting commit | `81a6dcd2c7` (HEAD of `BroadleafCommerce-4.0.x`) |
| Original Java source/target | `1.7` / `1.7` |
| Maven version | 3.6.3 (upgraded via wrapper to 3.9.6) |
| Baseline tests green before upgrade | No - Groovy 2.3 / Spock 1.x incompatible with Java 21; EasyMock 2.x cglib incompatible with class file version 65 |

## Target

| Item | Value |
|------|-------|
| Java version | 21 (LTS) - OpenJDK 21.0.10 |
| Compiler configuration | `<release>21</release>` via maven-compiler-plugin 3.13.0 |
| Enforcer rule | maven-enforcer-plugin 3.5.0 requiring `[21,)` |

### Maven Plugin Versions

| Plugin | Old | New |
|--------|-----|-----|
| maven-compiler-plugin | 3.1 | 3.13.0 |
| maven-surefire-plugin | 2.12.4 | 3.3.1 |
| maven-failsafe-plugin | (none) | 3.3.1 |
| maven-resources-plugin | (none) | 3.3.1 |
| maven-jar-plugin | 2.3.2 | 3.4.2 |
| maven-war-plugin | 2.1.1 | 3.4.0 |
| maven-install-plugin | (none) | 3.1.2 |
| maven-deploy-plugin | 2.7 | 3.1.2 |
| jacoco-maven-plugin | 0.7.2 | 0.8.12 |
| gmavenplus-plugin | 1.5 | 3.0.2 |
| maven-enforcer-plugin | (none) | 3.5.0 |

### Key Dependency Changes

| Dependency | Old | New | Reason |
|------------|-----|-----|--------|
| Spring Framework | 4.1.9.RELEASE | 5.3.39 | Java 21 support |
| Spring Security | 3.2.9.RELEASE | 5.8.15 | Spring 5 compatibility |
| Hibernate ORM | 4.1.11.Final | 5.6.15.Final | Java 21 + JPA 2.1 |
| Thymeleaf | 2.1.4.RELEASE | 3.0.15.RELEASE | Java 21 + Spring 5 |
| Groovy | 2.3.10 (org.codehaus) | 4.0.24 (org.apache) | Java 21 support |
| Spock | 1.3-groovy-2.5 | 2.3-groovy-4.0 | Groovy 4 compatibility |
| Geb | 2.3 | 7.0 | Selenium 4 + Groovy 4 |
| Selenium | 3.141.59 | 4.18.1 | Java 21 support |
| EasyMock | 2.5.1 | 5.2.0 | cglib Java 21 support |
| cglib-nodep | 2.2 | 3.3.0 | Java 21 class file version |
| ASM | (transitive) | 9.7 | Java 21 bytecode support |
| javax.annotation-api | (JDK built-in) | 1.3.2 | Removed from JDK 11+ |
| javax.xml.bind-api | (JDK built-in) | 2.3.1 | Removed from JDK 11+ |
| JAXB Runtime | (JDK built-in) | 2.3.9 | Removed from JDK 11+ |
| javassist | 3.18.1-GA | 3.30.2-GA | Java 21 support |

## Breaking Changes and Fixes

### Thymeleaf 2 to 3 Migration (~50 processor files)
- `AbstractAttrProcessor` / `AbstractElementProcessor` replaced by `AbstractAttributeTagProcessor` / `AbstractElementTagProcessor`
- Constructor patterns changed to include `TemplateMode`, dialect prefix, precedence
- `ProcessorResult processAttribute(Arguments, Element, String)` replaced by `void doProcess(ITemplateContext, IProcessableElementTag, AttributeName, String, IElementTagStructureHandler)`
- `Arguments` replaced by `ITemplateContext` throughout
- `Element` replaced by `IProcessableElementTag` (read-only)
- DOM manipulation via `Element.setAttribute()` replaced by `structureHandler.setAttribute()`
- `IResourceResolver` interface removed; `DatabaseResourceResolver` converted to plain service
- `TemplateResolver` replaced by `AbstractConfigurableTemplateResolver`
- `TemplateProcessingParameters` removed from resolver APIs

### Hibernate 4 to 5
- `org.hibernate.ejb.EntityManagerFactory` removed; use `SessionFactory.unwrap()` pattern
- `@org.hibernate.annotations.Index` must be fully qualified (conflicts with `javax.persistence.Index`)
- Various internal API removals

### Spring 4 to 5
- `@TransactionConfiguration` removed; replaced with `@Transactional` + `@Rollback`
- `TransactionConfigurationAttributes` removed; hardcoded defaults in `MergeTransactionalTestExecutionListener`

### Spring Security 3 to 5
- `ProviderSignInUtils` changed from static to instance methods
- `getConnection()` renamed to `getConnectionFromSession()`
- `handlePostSignUp()` renamed to `doPostSignUp()`

### Servlet API
- `HttpServletResponse` gained abstract methods in Servlet 3.1: `getHeader()`, `getHeaders()`, `getHeaderNames()`, `getStatus()`, `setContentLengthLong()`
- Added implementations in `BroadleafResponseWrapper`

### EasyMock
- `easymockclassextension` merged into `easymock` core (since 3.2)
- `org.easymock.classextension.EasyMock` imports changed to `org.easymock.EasyMock`

### Java Module System
- Added `--add-opens` JVM args in surefire/failsafe for reflective access required by Hibernate, Spring, and MVEL

## Verification Commands

```bash
# Full test suite (must pass)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test

# Compile only
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -DskipTests compile

# Full verify (includes integration tests if configured)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw verify
```

Expected outcome: BUILD SUCCESS across all 13 modules.

## Commit History

1. `1372768e28` - Toolchain: require Java 21 and add Maven wrapper
2. `a37488c81b` - Plugins: upgrade surefire/failsafe/jacoco and migrate to Groovy 4 + Spock 2
3. `9535f9da57` - Dependencies: modernize framework stack for Java 21 compatibility
4. `50ab42c053` - Code fixes: compilation + runtime compatibility for Java 21
5. `080f903327` - Tests: fix test compilation and runtime for Java 21
6. (this commit) - Docs: upgrade report and local runbook
