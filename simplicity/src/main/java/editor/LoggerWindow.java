package editor;

import java.util.ArrayList;
import java.util.List;
import imgui.*;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.*;
import logger.Log;

public class LoggerWindow extends ImGuiInterface {

    private class Entry {
        private static int lineStatic = 1;
        public short r, g, b, a;
        public int line;
        public String text;

        public Entry(String text) {
            this.text = text;
            this.r = 255;
            this.g = 255;
            this.b = 255;
            this.a = 127;
            this.line = lineStatic++;
        }

        public Entry(String text, int r, int g, int b, int a) {
            this.text = text;
            this.r = (short) r;
            this.g = (short) g;
            this.b = (short) b;
            this.a = (short) a;
            this.line = lineStatic++;
        }
    }

    private ImString textInput;
    private boolean scrollBotton, autoScroll;
    private List<Entry> entries;

    public LoggerWindow() {
        textInput = new ImString("", 256);
        scrollBotton = false;
        autoScroll = false;
        entries = new ArrayList<>();
    }

    public void print(final String message) {
        entries.add(new Entry(message));
    }

    private void print(final ImString message) {
        entries.add(new Entry(message.get()));
    }
    
    @Override
    public void imgui() {
        ImGui.begin("Terminal");
        updateCalc();

        // ImGui.button("CLICK", 100, 20);

        logArea();

        ImGui.separator();

        inputArea();
        ImGui.end();
    }

    private void logArea() {
        final float footer = ImGui.getStyle().getItemInnerSpacingY() + ImGui.getFrameHeightWithSpacing();
        ImGui.beginChild("##", 0, -footer);

        for (Entry entry : entries) {
            ImGui.textColored(entry.r, entry.g, entry.b, entry.a, entry.text);
        }

        if ((scrollBotton && (ImGui.getScrollY() >= ImGui.getScrollMaxY() || autoScroll)))
            ImGui.setScrollHereY(1.0f);
        scrollBotton = false;

        ImGui.endChild();
    }

    private void inputArea() {
        final String label = ">";
        ImGui.pushID(label);
        ImGui.columns(2);
        ImGui.setColumnWidth(0, 20f);
        ImGui.text(label);
        ImGui.nextColumn();

        if (ImGui.inputTextWithHint("##" + label, "Enter Command", textInput, ImGuiInputTextFlags.EnterReturnsTrue)) {
            if (textInput != null && textInput.isNotEmpty()) {
                print(textInput);
                textInput.clear();
            }
            ImGui.columns(1);
            ImGui.popID();
        } else {
            ImGui.columns(1);
            ImGui.popID();    
        }
        ImGui.sameLine();
        if (ImGui.button("Clear Terminal")) {
            entries.clear();
        }
    }
    
    @Override
    public void destroy() {

    }

    public void log(Log log) {
        switch (log.getLogLevel()) {
            case INFO:
                entries.add(new Entry(log.toString()));
                break;
            case WARN:
                entries.add(new Entry(log.toString(), 238, 210, 2, 127));
                break;
            case ERROR:
            case FATAL:
                entries.add(new Entry(log.toString(), 255, 50, 50, 127));
                break;
            default:
                break;
        }
        scrollBotton = true;
    }
    
}