using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class Interrogation3D : MonoBehaviour
{
    Camera cam; Transform suspect; Text status; Text dialogue; Canvas canvas;
    bool busy; int trust=50; int questions=0;
    Material Mat(Color c, float metallic=0f, float smooth=.35f) {
        var m=new Material(Shader.Find("Standard")); m.color=c; m.SetFloat("_Metallic",metallic); m.SetFloat("_Glossiness",smooth); return m;
    }
    GameObject Cube(string n, Vector3 p, Vector3 s, Material m) { var g=GameObject.CreatePrimitive(PrimitiveType.Cube); g.name=n; g.transform.position=p; g.transform.localScale=s; g.GetComponent<Renderer>().material=m; return g; }
    GameObject Ball(string n, Vector3 p, Vector3 s, Material m) { var g=GameObject.CreatePrimitive(PrimitiveType.Sphere); g.name=n; g.transform.position=p; g.transform.localScale=s; g.GetComponent<Renderer>().material=m; return g; }
    GameObject Cylinder(string n, Vector3 p, Vector3 s, Material m) { var g=GameObject.CreatePrimitive(PrimitiveType.Cylinder); g.name=n; g.transform.position=p; g.transform.localScale=s; g.GetComponent<Renderer>().material=m; return g; }
    void Start() {
        Application.targetFrameRate=60;
        var floor=Mat(new Color(.22f,.24f,.25f)); var wall=Mat(new Color(.36f,.39f,.40f)); var dark=Mat(new Color(.09f,.11f,.13f));
        var wood=Mat(new Color(.24f,.15f,.10f)); var metal=Mat(new Color(.22f,.25f,.27f),.8f,.7f);
        var skin=Mat(new Color(.72f,.48f,.36f)); var hair=Mat(new Color(.09f,.055f,.045f)); var shirt=Mat(new Color(.28f,.32f,.39f)); var pants=Mat(new Color(.12f,.14f,.18f));
        Cube("Floor",new Vector3(0,-.12f,0),new Vector3(6,.2f,7),floor);
        Cube("Back wall",new Vector3(0,1.7f,3.5f),new Vector3(6,3.6f,.2f),wall);
        Cube("Left wall",new Vector3(-3,1.7f,0),new Vector3(.2f,3.6f,7),wall);
        Cube("Right wall",new Vector3(3,1.7f,0),new Vector3(.2f,3.6f,7),wall);
        Cube("Ceiling",new Vector3(0,3.5f,0),new Vector3(6,.15f,7),dark);
        Cube("One-way mirror",new Vector3(0,2.25f,3.37f),new Vector3(2.4f,1.15f,.025f),Mat(new Color(.12f,.17f,.19f),.9f,.9f));
        Cube("Table top",new Vector3(0,1.05f,.4f),new Vector3(2.5f,.13f,1.25f),wood);
        foreach(float x in new float[]{-1.05f,1.05f}) foreach(float z in new float[]{-.05f,.85f}) Cube("Table leg",new Vector3(x,.52f,z),new Vector3(.12f,1f,.12f),metal);
        Cube("Ceiling light",new Vector3(0,3.38f,.2f),new Vector3(1.4f,.04f,.55f),Mat(new Color(.95f,.82f,.61f),0,.8f));
        var light=new GameObject("Warm ceiling light").AddComponent<Light>(); light.type=LightType.Spot; light.transform.position=new Vector3(0,3.25f,.2f); light.transform.rotation=Quaternion.Euler(90,0,0); light.range=8; light.spotAngle=95; light.intensity=4.2f; light.color=new Color(1f,.82f,.66f);
        RenderSettings.ambientLight=new Color(.34f,.34f,.36f);
        // Seated suspect: articulated primitive 3D character, deliberately non-graphic.
        suspect=new GameObject("Suspect").transform; suspect.position=new Vector3(0,0,2.05f);
        var torso=Cube("Torso",suspect.position+new Vector3(0,1.28f,0),new Vector3(.65f,.82f,.36f),shirt); torso.transform.SetParent(suspect);
        var head=Ball("Head",suspect.position+new Vector3(0,2.02f,0),new Vector3(.43f,.53f,.4f),skin); head.transform.SetParent(suspect);
        var haircap=Ball("Hair",suspect.position+new Vector3(0,2.23f,-.035f),new Vector3(.45f,.28f,.42f),hair); haircap.transform.SetParent(suspect);
        foreach(float x in new float[]{-.105f,.105f}) { var eye=Ball("Eye",suspect.position+new Vector3(x,2.07f,.19f),new Vector3(.045f,.045f,.025f),dark); eye.transform.SetParent(suspect); }
        var nose=Ball("Nose",suspect.position+new Vector3(0,1.98f,.21f),new Vector3(.07f,.11f,.07f),skin); nose.transform.SetParent(suspect);
        foreach(float x in new float[]{-.2f,.2f}) { var arm=Cube("Arm",suspect.position+new Vector3(x,1.32f,.03f),new Vector3(.19f,.7f,.2f),shirt); arm.transform.localRotation=Quaternion.Euler(0,0,x<0?-10:10); arm.transform.SetParent(suspect); var hand=Ball("Hand",suspect.position+new Vector3(x,.98f,.18f),new Vector3(.16f,.16f,.14f),skin); hand.transform.SetParent(suspect); }
        foreach(float x in new float[]{-.18f,.18f}) { var leg=Cube("Leg",suspect.position+new Vector3(x,.48f,-.02f),new Vector3(.24f,.85f,.28f),pants); leg.transform.SetParent(suspect); }
        Cube("Chair seat",new Vector3(0,.65f,2.12f),new Vector3(.9f,.12f,.8f),dark); Cube("Chair back",new Vector3(0,1.15f,2.48f),new Vector3(.9f,1.1f,.12f),dark);
        var player=new GameObject("First person camera"); player.transform.position=new Vector3(0,1.62f,-2.1f); player.transform.rotation=Quaternion.Euler(0,0,0); cam=player.AddComponent<Camera>(); cam.fieldOfView=65; cam.nearClipPlane=.05f; cam.farClipPlane=100;
        BuildUI();
        SetDialogue("Siz so‘roq xonasidasiz. Gumonlanuvchi bilan gaplashing.");
    }
    void BuildUI() {
        var go=new GameObject("Mobile UI"); canvas=go.AddComponent<Canvas>(); canvas.renderMode=RenderMode.ScreenSpaceOverlay; go.AddComponent<CanvasScaler>().uiScaleMode=CanvasScaler.ScaleMode.ScaleWithScreenSize; go.GetComponent<CanvasScaler>().referenceResolution=new Vector2(1080,1920); go.AddComponent<GraphicRaycaster>();
        var panel=new GameObject("Dialogue panel"); panel.transform.SetParent(canvas.transform,false); var img=panel.AddComponent<Image>(); img.color=new Color(.03f,.04f,.05f,.86f); var rt=panel.GetComponent<RectTransform>(); rt.anchorMin=new Vector2(.05f,.72f); rt.anchorMax=new Vector2(.95f,.96f); rt.offsetMin=rt.offsetMax=Vector2.zero;
        dialogue=MakeText(panel.transform,"Dialogue",new Vector2(.05f,.25f),new Vector2(.95f,.9f),30,TextAnchor.MiddleCenter);
        status=MakeText(panel.transform,"Status",new Vector2(.05f,.02f),new Vector2(.95f,.22f),23,TextAnchor.MiddleCenter);
        AddButton("SAVOL BERISH",new Vector2(.05f,.08f),new Vector2(.47f,.17f),()=>Ask());
        AddButton("DALIL KO‘RSATISH",new Vector2(.53f,.08f),new Vector2(.95f,.17f),()=>Evidence());
        AddButton("TINCHLANTIRISH",new Vector2(.05f,.01f),new Vector2(.47f,.075f),()=>Calm());
        AddButton("QAT’IY OHANG",new Vector2(.53f,.01f),new Vector2(.95f,.075f),()=>Firm());
    }
    Text MakeText(Transform parent,string n,Vector2 a,Vector2 b,int size,TextAnchor align) { var g=new GameObject(n); g.transform.SetParent(parent,false); var t=g.AddComponent<Text>(); t.font=Resources.GetBuiltinResource<Font>("Arial.ttf"); t.fontSize=size; t.alignment=align; t.color=Color.white; var r=t.rectTransform; r.anchorMin=a; r.anchorMax=b; r.offsetMin=r.offsetMax=Vector2.zero; return t; }
    void AddButton(string label,Vector2 min,Vector2 max,UnityEngine.Events.UnityAction act) { var g=new GameObject(label); g.transform.SetParent(canvas.transform,false); var im=g.AddComponent<Image>(); im.color=new Color(.12f,.22f,.29f,.96f); var b=g.AddComponent<Button>(); b.targetGraphic=im; b.onClick.AddListener(act); var r=g.GetComponent<RectTransform>(); r.anchorMin=min; r.anchorMax=max; r.offsetMin=r.offsetMax=Vector2.zero; var t=MakeText(g.transform,"Label",Vector2.zero,Vector2.one,25,TextAnchor.MiddleCenter); t.text=label; }
    void SetDialogue(string s) { if(dialogue) dialogue.text=s; if(status) status.text="ISHONCH: "+trust+"%   |   SAVOLLAR: "+questions; }
    void Ask() { questions++; trust=Mathf.Clamp(trust+2,0,100); SetDialogue(new[]{"Qayerda edingiz voqea sodir bo‘lgan paytda?","Bu ma’lumotni yana bir bor tushuntirib bera olasizmi?","Guvohlarning ko‘rsatmalari bilan javobingiz mos kelmayapti."}[questions%3]); }
    void Evidence() { trust=Mathf.Clamp(trust-3,0,100); SetDialogue("Dalil stol ustiga qo‘yildi. Gumonlanuvchi jim bo‘lib qoldi va hujjatga qaradi."); }
    void Calm() { trust=Mathf.Clamp(trust+8,0,100); SetDialogue("Iltimos, xotirjam bo‘ling. Sizni tinglayapman. Hammasini boshidan aytib bering."); }
    void Firm() { trust=Mathf.Clamp(trust-5,0,100); SetDialogue("Savolga aniq javob bering. Vaqt va joyni yana tekshiramiz."); }
}
