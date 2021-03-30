FROM openjdk:8-jre
ENV MINISOCIAL_ROOT_DIR=/minisocial

RUN mkdir -p $MINISOCIAL_ROOT_DIR

WORKDIR $MINISOCIAL_ROOT_DIR
RUN chmod -R 777 $MINISOCIAL_ROOT_DIR
COPY ./target/minisocial-0.0.1-SNAPSHOT.jar minisocial.jar

WORKDIR /
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod a+x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

