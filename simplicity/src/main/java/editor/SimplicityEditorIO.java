package editor;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import components.Component;
import components.ComponentDeserializer;
import imgui.app.Application;
import scenes.LevelEditorSceneInitializer;
import simplicity.GameObject;
import simplicity.GameObjectDeserializer;

public class SimplicityEditorIO {

    // public void saveWorld() {

    // }

    // public void save(Application context) {
    //     if (Scene.currentFile != null) {
    //         saveAs(Scene.currentFile);
    //     } else {
    //         String path = util.IOHelper.saveFile(OldWindow.get(), "level", "json");
    //         saveAs(path);
    //         OldWindow.changeScene(new LevelEditorSceneInitializer(path));
    //     }
    // }

    // public void saveAs(String filepath) {
    //     if (filepath != null) {
    //         Gson gson = new GsonBuilder()
    //         .setPrettyPrinting()
    //         .registerTypeAdapter(Component.class, new ComponentDeserializer())
    //         .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
    //         .create();
    //         try {
    //             FileWriter writer = new FileWriter(filepath);
    //             List<GameObject> objsToSerialize = new ArrayList<>();
    //             for(GameObject go : this.gameObjects) {
    //                 if(go.doSerialization()) {
    //                     objsToSerialize.add(go);
    //                 }
    //             }
    //             writer.write(gson.toJson(objsToSerialize));
    //             writer.close();
    //             logger.Logger.info("Successfully saved '" + levelName + "'");
    //         } catch(IOException e) {
    //             logger.Logger.error("Unable to save '" + levelName + "'");
    //             e.printStackTrace();
    //         }
    //     }
    // }

    // public void load() {
    //     load(Scene.currentFile);
    // }

    // private void load(String filepath) {
    //     Gson gson = new GsonBuilder()
    //     .setPrettyPrinting()
    //     .registerTypeAdapter(Component.class, new ComponentDeserializer())
    //     .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
    //     .create();

    //     String inFile = "";
    //     try {
    //         inFile = filepath == null ? "" : new String(Files.readAllBytes(Paths.get(filepath)));
    //         if (filepath != null) {
    //             logger.Logger.info("Successfully loaded '" + filepath + "'");
    //         }
    //     } catch(IOException e) {
    //         logger.Logger.error("Unable to load '" + filepath + "'");
    //         e.printStackTrace();
    //     }
    //     if(!inFile.equals("")) {
    //         int maxGoId = -1;
    //         int maxCompId = -1;
    //         GameObject[] objs = gson.fromJson(inFile, GameObject[].class);
    //         for(int i = 0; i < objs.length; i++) {
    //             addGameObjectToScene(objs[i]);

    //             for(Component c : objs[i].getAllComponenets()) {
    //                 if(c.getUid() > maxCompId) {
    //                     maxCompId = c.getUid();
    //                 }
    //             }
    //             if(objs[i].getUid() > maxGoId) {
    //                 maxGoId = objs[i].getUid();
    //             }
    //         }

    //         maxGoId++;
    //         maxCompId++;
    //         GameObject.init(maxGoId);
    //         Component.init(maxCompId);
    //     }
    // }
}
