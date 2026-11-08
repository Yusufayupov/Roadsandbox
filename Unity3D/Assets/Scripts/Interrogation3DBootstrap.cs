using UnityEngine;

public static class Interrogation3DBootstrap
{
    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
    static void CreateGame()
    {
        if (Object.FindObjectOfType<Interrogation3D>() != null) return;
        var root = new GameObject("Interrogation 3D Game");
        root.AddComponent<Interrogation3D>();
    }
}
