package com.example.ems.cli;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
public class ConsoleIO {
    private final Charset charset;
    private final BufferedReader in;
    private final PrintWriter out;
    public ConsoleIO() {
        this.charset = resolveCharset();
        this.in = new BufferedReader(new InputStreamReader(System.in, charset));
        this.out = new PrintWriter(new OutputStreamWriter(System.out, charset), true);
    }
    private Charset resolveCharset() {
        String forced = System.getProperty("ems.cli.charset");
        if (forced != null && !forced.isBlank()) {
            try {
                return Charset.forName(forced.trim());
            } catch (Exception ignored) {}
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("windows")) {
            try {
                return Charset.forName("MS932");
            } catch (Exception ignored) {}
            try {
                return Charset.forName("Windows-31J");
            } catch (Exception ignored) {}
            try {
                return Charset.forName("Shift_JIS");
            } catch (Exception ignored) {}
        }
        return Charset.forName("UTF-8");
    }
    public Charset getCharset() {
        return charset;
    }
    public void println(String s) {
        out.println(s);
    }
    public void print(String s) {
        out.print(s);
        out.flush();
    }
    public void blank() {
        out.println();
    }
    public String readLine(String prompt) {
        try {
            if (prompt != null && !prompt.isEmpty()) {
                print(prompt);
            }
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }
    public String readNonEmpty(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (s == null) return null;
            s = s.trim();
            if (!s.isEmpty()) return s;
            println("空欄は不可です。");
        }
    }
    public int readIntInRange(String prompt, int min, int max, int defaultValue) {
        while (true) {
            String s = readLine(prompt);
            if (s == null) return defaultValue;
            s = s.trim();
            if (s.isEmpty()) return defaultValue;
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) {
                    println(min + "〜" + max + "で入力してください。");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                println("数字で入力してください。");
            }
        }
    }
    public void pause() {
        readLine("\nEnterで戻る...");
    }
    public void pause(String message) {
        readLine("\n" + (message == null ? "Enterで戻る..." : message));
    }
}