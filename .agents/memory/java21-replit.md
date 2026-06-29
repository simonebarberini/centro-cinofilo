---
name: Java 21 in Replit
description: Come compilare con Java 21 in questo ambiente Replit che ha GraalVM 22.3 (Java 19) come default.
---

## Regola

Per compilare il progetto backend (pom.xml richiede Java 21), impostare esplicitamente JAVA_HOME prima di eseguire Maven:

```bash
export JAVA_HOME=/nix/store/k95pqfzyvrna93hc9a4cg5csl7l4fh0d-openjdk-21.0.7+6
export PATH=$JAVA_HOME/bin:$PATH
cd backend && mvn <goal>
```

**Why:** Il modulo Replit `java-graalvm22.3` mette in PATH GraalVM 22.3 (Java 19) che non supporta `--release 21`. Java 21 (OpenJDK 21.0.7+6) è installato via Nix (`installSystemDependencies jdk21`) ed è presente nel PATH ma in posizione inferiore. Il file `.replit` non può essere editato direttamente.

**How to apply:** Ogni volta che si esegue `mvn compile`, `mvn test`, `mvn package` o qualsiasi goal Maven sul backend.

Il Nix store path `k95pqfzyvrna93hc9a4cg5csl7l4fh0d` è deterministico per OpenJDK 21.0.7+6 sul channel `stable-25_05` — non cambierà salvo upgrade del channel.
