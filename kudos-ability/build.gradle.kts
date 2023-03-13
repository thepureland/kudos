// 为了不生成src、resources、test、testresources四个目录
sourceSets {
    main {
        kotlin {
            setSrcDirs(emptyList<String>())
        }
        java {
            setSrcDirs(emptyList<String>())
        }
        resources {
            setSrcDirs(emptyList<String>())
        }
    }
    test {
        kotlin {
            setSrcDirs(emptyList<String>())
        }
        java {
            setSrcDirs(emptyList<String>())
        }
        resources {
            setSrcDirs(emptyList<String>())
        }
    }
}

