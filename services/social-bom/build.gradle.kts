plugins {
    `java-platform`
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    // quarkus-amazon-services-bom KHÔNG import ở đây nữa — bản mới nhất (3.22.3)
    // vẫn khác "platform stream" với quarkus-bom:3.25.1, Quarkus curateOutcome
    // reject combo này (xem specs/decisions/0002). Import platform này chỉ ở
    // post-service/post-api (2 module thực sự cần S3) để không chặn build của
    // 17 module còn lại không liên quan gì tới S3.

    constraints {
        api("cz.jirutka.rsql:rsql-parser:2.1.0")
        api("at.favre.lib:bcrypt:0.10.2")
    }
}
