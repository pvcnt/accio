FROM java:8
ADD app.jar /
EXPOSE 8888 9990
ENV JAVA_OPTS ""
ENTRYPOINT ["java","-server","-jar","/app.jar"]