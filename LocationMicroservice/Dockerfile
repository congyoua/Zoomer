FROM maven:3.6.3-jdk-8

WORKDIR /root/.m2/repository
COPY . ./
RUN mvn verify clean --fail-never
RUN mvn compile
ENTRYPOINT [ "mvn","exec:java" ]
EXPOSE 8000