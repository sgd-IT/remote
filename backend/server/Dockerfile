## AdoptOpenJDK 停止发布 OpenJDK 二进制，而 Eclipse Temurin 是它的延伸，提供更好的稳定性
## 感谢复旦核博士的建议！灰子哥，牛皮！
FROM eclipse-temurin:21-jre

## 创建目录，并使用它作为工作目录
RUN mkdir -p /iot-server /logs
WORKDIR /iot-server
## 将后端项目的 Jar 文件，复制到镜像中
COPY ./target/anwei-fire-remote-monitor.jar app.jar

## 设置 TZ 时区
ENV TZ=Asia/Shanghai
## 设置 JAVA_OPTS 环境变量，可通过 docker run -e "JAVA_OPTS=" 进行覆盖
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

## 应用参数
ENV ARGS=""

## 暴露端口：48080(HTTP) 1883(MQTT) 18834(TCP) 18888(UDP) 4444(CoAP)
EXPOSE 48080 1883 18834 18888 4444

## 启动后端项目
CMD java ${JAVA_OPTS} -jar app.jar $ARGS
