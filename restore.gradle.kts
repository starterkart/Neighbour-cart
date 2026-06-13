tasks.register<Copy>("restoreFiles") {
    from("assets/.aistudio/app/src/main/java")
    into("app/src/main/java")
}
