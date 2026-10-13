FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY src/main/java src/main/java
RUN mkdir -p /out && find src/main/java -name '*.java' > /tmp/sources.txt && javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d /out @/tmp/sources.txt

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /out /app/classes
USER 65532:65532
EXPOSE 8096
ENTRYPOINT ["java","-cp","/app/classes","io.ahad.mcpgateway.Main"]
