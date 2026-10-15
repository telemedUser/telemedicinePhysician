param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][int]$Port,
    [string]$PackageSuffix = "",
    [string]$Type = "service"  # service | bff | platform | simulator
)

$Root = Split-Path -Parent $PSScriptRoot
$Slug = $Name -replace '_', '-'
$ClassName = ($Slug -split '-' | ForEach-Object { $_.Substring(0,1).ToUpper() + $_.Substring(1) }) -join ''
$Pkg = if ($PackageSuffix) { $PackageSuffix } else { $Slug -replace '-', '' }

$TargetDir = switch ($Type) {
    "bff" { Join-Path $Root "bff\$Slug" }
    "platform" { Join-Path $Root "platform\$Slug" }
    "simulator" { Join-Path $Root "simulators\$Slug" }
    default { Join-Path $Root "services\$Slug" }
}

if (Test-Path $TargetDir) { Write-Error "Already exists: $TargetDir"; exit 1 }
New-Item -ItemType Directory -Force -Path "$TargetDir\src\main\java\com\telemedicine\$Pkg" | Out-Null
New-Item -ItemType Directory -Force -Path "$TargetDir\src\main\resources" | Out-Null
New-Item -ItemType Directory -Force -Path "$TargetDir\src\test\java\com\telemedicine\$Pkg" | Out-Null

$GradleModule = $Slug -replace '-', ''

@(
@'
plugins {
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

dependencies {
    implementation project(':java-common-core')
    implementation project(':java-common-web')
    implementation project(':java-common-security')
    implementation project(':java-common-events')
    implementation project(':java-common-observability')
    implementation project(':java-common-persistence')
    implementation project(':java-common-integration')
    implementation project(':java-common-audit')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.cloud:spring-cloud-starter-consul-discovery'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.flywaydb:flyway-core'
    runtimeOnly 'org.postgresql:postgresql'
    testImplementation project(':java-common-testing')
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

bootJar { archiveFileName = 'MODULE.jar' }
'@.Replace('MODULE', $GradleModule)
) | Set-Content "$TargetDir\build.gradle"

@(
@'
package com.telemedicine.PKG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CLASSApplication {
    public static void main(String[] args) {
        SpringApplication.run(CLASSApplication.class, args);
    }
}
'@.Replace('PKG', $Pkg).Replace('CLASS', $ClassName)
) | Set-Content "$TargetDir\src\main\java\com\telemedicine\$Pkg\${ClassName}Application.java"

@(
@'
package com.telemedicine.PKG;

import com.telemedicine.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("service", "SERVICE", "status", "UP"));
    }
}
'@.Replace('PKG', $Pkg).Replace('SERVICE', $Slug)
) | Set-Content "$TargetDir\src\main\java\com\telemedicine\$Pkg\HealthController.java"

@(
@'
server:
  port: PORT

spring:
  application:
    name: SERVICE
  cloud:
    consul:
      host: ${CONSUL_HOST:localhost}
      port: ${CONSUL_PORT:8500}
      discovery:
        enabled: true
        register: true
        prefer-ip-address: true
        health-check-path: /actuator/health
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:telemedicine}
    username: ${DB_USER:telemedicine}
    password: ${DB_PASSWORD:telemedicine}
  jpa:
    hibernate:
      ddl-auto: validate
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
'@.Replace('PORT', $Port).Replace('SERVICE', $Slug)
) | Set-Content "$TargetDir\src\main\resources\application.yml"

@(
@'
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY shared ./shared
COPY gateway ./gateway
COPY bff ./bff
COPY platform ./platform
COPY services ./services
COPY simulators ./simulators
RUN gradle :MODULE:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/TARGET/build/libs/MODULE.jar app.jar
EXPOSE SERVICE_PORT
ENTRYPOINT ["java", "-jar", "app.jar"]
'@.Replace('MODULE', $GradleModule).Replace('TARGET', (($TargetDir -replace [regex]::Escape($Root + '\'), '') -replace '\\', '/')).Replace('SERVICE_PORT', [string]$Port)
) | Set-Content "$TargetDir\Dockerfile"

Write-Host "Scaffolded $Slug at $TargetDir (port $Port)"
