package editor;

import imgui.*;
import imgui.extension.texteditor.*;
import imgui.flag.ImGuiWindowFlags;
import simplicity.Window;

import util.IOHelper;

public class TextEditorWindow extends ImGuiInterface {

    private TextEditor textEditor;
    private String currentFile, filename, saveTitle;
    // private boolean 

    public TextEditorWindow() {
        textEditor = new TextEditor();
        currentFile = null;
        saveTitle = "Save";
    }
    
    @Override
    public void imgui() {
        ImGui.begin("Text Editor", ImGuiWindowFlags.MenuBar);

        menuBar();

        ImGui.separator();
        
        textEditor.render("Editor");
        
        ImGui.end();
    }

    private void save() {
        if (currentFile != null) {
            IOHelper.WriteToFile(currentFile, textEditor.getText());
        } else {
            saveAs();
        }
    }

    private void menuBar() {
        ImGui.beginMenuBar();
        if(ImGui.menuItem(saveTitle, "", false, true)) {
            save();
        }
        if(ImGui.menuItem("Save As", "", false, true)) {
            saveAs();
        }
        if(ImGui.menuItem("Open", "", false, true)) {
            open();
        }
        ImGui.endMenuBar();
    }

    private void saveAs() {
        String filepath = IOHelper.saveFile(Window.get(), null, "txt");
        if (IOHelper.WriteToFile(filepath, textEditor.getText())) {
            currentFile = filepath;
            updateFilename();
        }
    }

    private void open() {
        String filepath = IOHelper.openSingle(Window.get(), "txt");
        String newText = IOHelper.ReadFromFile(filepath);
        if (newText != null) {
            currentFile = filepath;
            updateFilename();
            textEditor.setText(newText);
        }
    }

    private void updateFilename() {
        if (currentFile != null) {
            for (int i = currentFile.length() - 1; i >= 0; i--) {
                char c = currentFile.charAt(i);
                if (c == '/' || c == '\\') {
                    filename = currentFile.substring(i + 1);
                    saveTitle = "Save - "+ filename;
                    return;
                }
            }
        }
    }

    @Override
    public void destroy() {
        textEditor.destroy();
    }
    
}