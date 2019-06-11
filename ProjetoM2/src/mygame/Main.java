package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.AmbientLight;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.ui.Picture;
import java.util.ArrayList;
import java.util.List;

/**
 * Rafael Cortez Pereira
 * Lucas Eduardo Menassa
 * Rafael Inácio Ritter
 */

public class Main extends SimpleApplication implements ActionListener {
    private Spatial sceneModel;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
  
    // Jogador
    private CharacterControl player;
    private final Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    private final Vector3f camDir = new Vector3f();
    private final Vector3f camLeft = new Vector3f();
    private double playerLife = 100;
  
    // Inimigo
    private final Vector3f walkDirectionEnemy = new Vector3f();
    private float enemySpeed = 10f;
    private float lastEnemyTime = 0f;
    private float difficultyTime = 0f;
    private final List<Vector3f> positionEnemy = new ArrayList();
    private int qtdEnemy = 0;
    AnimControl control;
    AnimChannel channel;
    
    // Jogo
    BitmapText hudText;
    private int score = 0;
    private int scoringRecord = 0;
    private boolean startgame=false;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    
    private Node shootables;
    
    @Override
    public void simpleInitApp() {
        /** Configurando física */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // Posicionamento feito pela física. Camera reutilizada pra rotação.
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);
        setUpKeys();
        
        if(!startgame){
            initCrossHairs();   // função que configura a mira no meio da tela.
            initGun();          // função que configura a arma no canto da tela.
            setUpLight();       //função que configura a luz ambiente.
            startgame=true;
        }
        
        // Mapa carregado do .zip e ajustado na tela.
        assetManager.registerLocator("town.zip", ZipLocator.class);
        sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(2f);

        // A detecção de colisão é feita através de um RigidBodyControl com peso zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        // Configuração de tamanho, pulo e queda.
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);

        // Configurando gravidade e física.
        player.setGravity(new Vector3f(0,-55f,0));
        player.setPhysicsLocation(new Vector3f(0, 10, 0));
        
        shootables = new Node("Shootables");
        rootNode.attachChild(sceneModel);
    
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().add(player);
    
        rootNode.attachChild(shootables);
    
        // Valores para posição de adição do inimigo
        positionEnemy.add(new Vector3f(-75.8f, 1f, -24f));
        positionEnemy.add(new Vector3f(5.5f, 1f, 79f));
        positionEnemy.add(new Vector3f(58.3f, 1f, 79f));
        positionEnemy.add(new Vector3f(-2.5f, 1f, -114f));
        positionEnemy.add(new Vector3f(38f, 1f, -114f));
        positionEnemy.add(new Vector3f(78f, 1f, -114f));
    
        // HUD
        hudText = new BitmapText(guiFont, false);
        hudText.setSize(guiFont.getCharSet().getRenderedSize()+6);          // font size
        hudText.setColor(ColorRGBA.Yellow);                                   // font color
        hudText.setLocalTranslation(20, this.settings.getHeight() - 20, 0); // position
        guiNode.attachChild(hudText);
    }
    
    // configuração de Luz ambiente e luz direcional.
    private void setUpLight() {
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);
        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }
    
    // Configuração de quais teclas serão utilizadas.
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        inputManager.addListener(this, "Shoot"); 
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
    }
    
    // Configuração de cada tecla que será utilizada.
    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        switch (binding) {
            case "Left":
                left = isPressed;
                break;
            case "Right":
                right= isPressed;
                break;
            case "Up":
                up = isPressed;
                break;
            case "Down":
                down = isPressed;
                break;
            case "Jump":
                if (isPressed) {
                    if(cam.getLocation().getY() <= 6f){
                        player.jump(new Vector3f(0,18f,0));
                    }
                }
                // Colisão do inimigo com o tiro.
                break;
            case "Shoot":
                CollisionResults results = new CollisionResults();
                Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                for(Spatial s : shootables.getChildren()){
                    s.collideWith(ray, results);
                    if (results.size() > 0){
                        s.getControl(AnimControl.class).getChannel(0).setAnim("Death1");
                        s.getControl(AnimControl.class).getChannel(0).setLoopMode(LoopMode.Cycle);
                        
                        score+= 10;
                        
                        if(score > scoringRecord){
                            scoringRecord = score;
                        }
                        
                        shootables.detachChild(s);
                        bulletAppState.getPhysicsSpace().remove(s);
                        break;
                    }
                }   break;
            default:
                break;
        }
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        camDir.set(cam.getDirection()).multLocal(0.6f);
        camLeft.set(cam.getLeft()).multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        walkDirectionEnemy.set(0, 0, 0);
        
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
        
        if(timer.getTimeInSeconds() > lastEnemyTime + 1){
            lastEnemyTime = timer.getTimeInSeconds();
            shootables.attachChild(makeCharacter());
        }
        
        if(timer.getTimeInSeconds() > difficultyTime + 40 && enemySpeed <= 60){
            difficultyTime = timer.getTimeInSeconds();
            enemySpeed = enemySpeed + 10;
        }
        
        hudText.setText("SCORE: " + score + "\nLIFE: " + (int)playerLife + "\nRECORD: " + scoringRecord);
         
        // Movimentação do inimigo.
        for(Spatial s : shootables.getChildren()){
            enemyActions(s, tpf);
        }
        
        // Condição de fim de jogo quando a vida chegar a zero.
        if(playerLife <= 0){
            resetVariables();
        }
    }
    
    /** Configuração da mira na tela. */
    protected void initCrossHairs() {
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
        settings.getWidth() / 2 - ch.getLineWidth()/2,
        settings.getHeight() / 2 + ch.getLineHeight()/2, 0);
        guiNode.attachChild(ch);
    }
    
    /** Configuração da arma na tela. */
    protected void initGun() {
        Picture pic = new Picture("Imagem");
        pic.setImage(assetManager, "gun.png", true);
        pic.setWidth(settings.getWidth()-50);
        pic.setHeight(settings.getHeight()-30);
        pic.setPosition(settings.getWidth()-675, settings.getHeight()-675);
        guiNode.attachChild(pic);
    }
    
    /** Criação e configuração do inimigo no cenário
     * @return  */
    protected Spatial makeCharacter() {
        Spatial enemy = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        enemy.scale(0.025f);
        enemy.setLocalTranslation(positionEnemy.get(qtdEnemy%6)); 
        qtdEnemy++;
        
        control = enemy.getControl(AnimControl.class);
        channel = control.createChannel();
        channel.setAnim("Walk", 0.50f);
        channel.setLoopMode(LoopMode.Loop);
        return enemy;
    }
    
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals("Attack3")) {
            channel.setAnim("Walk", 0.50f);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(1f);
        }
    }
    
    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        // unused
    }
    
    // Configuração de ações dos inimigos.
    public void enemyActions(Spatial s, float tpf){
        s.lookAt(cam.getLocation(),  Vector3f.UNIT_Y.normalize());
        s.rotate(0, (float) Math.PI , 0);
        Vector3f dir = s.getLocalTranslation().subtract(cam.getLocation()).normalize().negate();
        if(cam.getLocation().subtract(s.getLocalTranslation()).getX() < 5
        && cam.getLocation().subtract(s.getLocalTranslation()).getX() > -5
        && cam.getLocation().subtract(s.getLocalTranslation()).getZ() < 5
        && cam.getLocation().subtract(s.getLocalTranslation()).getZ() > -5){
            s.move(dir.x*0*tpf, 0, dir.z*0*tpf);
            if(!s.getControl(AnimControl.class).getChannel(0).getAnimationName().equals("Attack3")
            && !s.getControl(AnimControl.class).getChannel(0).getAnimationName().equals("Death1")){
                s.getControl(AnimControl.class).getChannel(0).setAnim("Attack3");
                s.getControl(AnimControl.class).getChannel(0).setLoopMode(LoopMode.Cycle);
            }
            
            playerLife -= 0.02; // Vida do jogador diminui quando os inimigos estão perto demais.
        }
        else {
            s.move(dir.x*enemySpeed*tpf, 0, dir.z*enemySpeed*tpf);
            if(!s.getControl(AnimControl.class).getChannel(0).getAnimationName().equals("Walk")
            && !s.getControl(AnimControl.class).getChannel(0).getAnimationName().equals("Death1")){
                s.getControl(AnimControl.class).getChannel(0).setAnim("Walk");
                s.getControl(AnimControl.class).getChannel(0).setLoopMode(LoopMode.Loop);
            }
        }
    }
    
    // Configuração de reset de variáveis para reinicio do jogo.
    public void resetVariables() {
        shootables.detachAllChildren();
        rootNode.detachAllChildren();
        hudText.setText("");
        enemySpeed = 10f;
        playerLife = 100;
        score = 0;
        simpleInitApp();
    }
}