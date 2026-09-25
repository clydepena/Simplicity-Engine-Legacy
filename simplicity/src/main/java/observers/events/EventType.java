package observers.events;

public enum EventType {
    
    Unset,
    ApplicationClosed,
    GameEngineStartPlay,
    GameEngineStopPlay,
    SaveLevel,
    SaveLevelAs,
    LoadLevel,
    UserEvent,
    EventLogged,

    FramebufferResize,
    WindowClose,
    KeyInput,
    CharInput,
    MouseButton,
    MouseMoved,
    MouseScroll,
    MouseDroppedPath
    
}
