plugins {
    scala
}

repositories {
    mavenCentral()
}

// tag::zinc-dependency[]
dependencies {
    zinc("com.typesafe.zinc:zinc:0.3.9")
}
// end::zinc-dependency[]
