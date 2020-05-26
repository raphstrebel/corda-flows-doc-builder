package com.github.lucacampanella.callgraphflows;

import java.io.IOException;

import javassist.NotFoundException;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void main() throws IOException, NotFoundException {
        final String jarPath = getClass().getClassLoader().getResource("KotlinTestJar.jar").getPath();
        Main.main(new String[]{jarPath, "-o", "build/graphs", "-d", "fernflower", "-l", "--no-box-subflows",
        "--draw-return", "--draw-statements-with-relevant-methods"});
    }
}