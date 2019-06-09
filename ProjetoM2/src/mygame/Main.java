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
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;



/**
 * Example 9 - How to make walls and floors solid.
 * This collision code uses Physics and a custom Action Listener.
 * @author normen, with edits by Zathras
 */
public class Main extends SimpleApplication
        implements ActionListener {

  private Spatial sceneModel;
  private BulletAppState bulletAppState;
  private RigidBodyControl landscape;
  
  //Player
  private CharacterControl player;
  private Vector3f walkDirection = new Vector3f();
  private boolean left = false, right = false, up = false, down = false;
  private Vector3f camDir = new Vector3f();
  private Vector3f camLeft = new Vector3f();
  private int playerLife = 200;
  
  //Enemy
  private Vector3f walkDirectionEnemy = new Vector3f();
  private float enemySpeed = 5f;
  private float lastEnemyTime = 0f;
  private float difficultyTime = 0f;
  private List<Vector3f> positionEnemy = new ArrayList();
  private int qtdEnemy = 0;
  AnimControl control;
  AnimChannel channel;
  
  
  //Game
  BitmapText hudText;
  private int score = 0;
  private int scoringRecord = 0;

  public static void main(String[] args) {
    Main app = new Main();
    app.start();
  }
  
  private Node shootables;
  private Geometry mark;

  @Override
  public void simpleInitApp() {
      
    initCrossHairs(); // a "+" in the middle of the screen to help aiming
      
    /** Set up Physics */
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);

    // We re-use the flyby camera for rotation, while positioning is handled by physics
    viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
    flyCam.setMoveSpeed(100);
    setUpKeys();
    setUpLight();

    // We load the scene from the zip file and adjust its size.
    assetManager.registerLocator("town.zip", ZipLocator.class);
    sceneModel = assetManager.loadModel("main.scene");
    sceneModel.setLocalScale(2f);

    // We set up collision detection for the scene by creating a
    // compound collision shape and a static RigidBodyControl with mass zero.
    CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
    landscape = new RigidBodyControl(sceneShape, 0);
    sceneModel.addControl(landscape);

    // We set up collision detection for the player by creating
    // a capsule collision shape and a CharacterControl.
    // The CharacterControl offers extra settings for
    // size, stepheight, jumping, falling, and gravity.
    // We also put the player in its starting position.
    CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
    player = new CharacterControl(capsuleShape, 0.05f);
    player.setJumpSpeed(20);
    player.setFallSpeed(30);

    //Some methods used for setting gravity related variables were deprecated in
    //the 3.2 version of the engine. Choose the method that matches your version
    //of the engine.
    player.setGravity(new Vector3f(0,-70f,0));
    player.setPhysicsLocation(new Vector3f(0, 10, 0));
    

    // We attach the scene and the player to the rootnode and the physics space,
    // to make them appear in the game world.
    shootables = new Node("Shootables");
    rootNode.attachChild(sceneModel);
    
    bulletAppState.getPhysicsSpace().add(landscape);
    bulletAppState.getPhysicsSpace().add(player);
    
    rootNode.attachChild(shootables);
    
    
    //Valres para posição de adição do inimigo
    positionEnemy.add(new Vector3f(-75.8f, 1f, -24f));
    positionEnemy.add(new Vector3f(5.5f, 1f, 79f));
    positionEnemy.add(new Vector3f(58.3f, 1f, 79f));
    positionEnemy.add(new Vector3f(-2.5f, 1f, -114f));
    positionEnemy.add(new Vector3f(38f, 1f, -114f));
    positionEnemy.add(new Vector3f(78f, 1f, -114f));
    
    
    //HUD
    hudText = new BitmapText(guiFont, false);
    hudText.setSize(guiFont.getCharSet().getRenderedSize());      // font size
    hudText.setColor(ColorRGBA.Yellow);                             // font color
    hudText.setLocalTranslation(20, this.settings.getHeight() - 20, 0); // position
    guiNode.attachChild(hudText);
  }

  private void setUpLight() {
    // We add light so we see the scene
    AmbientLight al = new AmbientLight();
    al.setColor(ColorRGBA.White.mult(1.3f));
    rootNode.addLight(al);

    DirectionalLight dl = new DirectionalLight();
    dl.setColor(ColorRGBA.White);
    dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
    rootNode.addLight(dl);
  }

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

  /** These are our custom actions triggered by key presses.
   * We do not walk yet, we just keep track of the direction the user pressed. */
  @Override
  public void onAction(String binding, boolean isPressed, float tpf) {
    if (binding.equals("Left")) {
      left = isPressed;
    } else if (binding.equals("Right")) {
      right= isPressed;
    } else if (binding.equals("Up")) {
      up = isPressed;
    } else if (binding.equals("Down")) {
      down = isPressed;
    } else if (binding.equals("Jump")) {
        if (isPressed) { 
            player.jump(new Vector3f(0,20f,0));
        }
    }else if (binding.equals("Shoot")) {
        //Colisão do inimigo com o tiro
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());

        for(Spatial s : shootables.getChildren()){
            s.collideWith(ray, results);
            if (results.size() > 0){
               s.getControl(AnimControl.class).getChannel(0).setAnim("Death1");
               s.getControl(AnimControl.class).getChannel(0).setLoopMode(LoopMode.Cycle);
         
                score+= 10;
                
                shootables.detachChild(s);
                bulletAppState.getPhysicsSpace().remove(s);
                break;
            }
         
        }
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
        
        if(timer.getTimeInSeconds() > lastEnemyTime + 5){
            lastEnemyTime = timer.getTimeInSeconds();
            shootables.attachChild(makeCharacter());
        }
        
        if(timer.getTimeInSeconds() > difficultyTime + 20 && enemySpeed <= 30){
            difficultyTime = timer.getTimeInSeconds();
            enemySpeed = enemySpeed + 5;
        }
        
        hudText.setText("SCORE: " + score + "\nLIFE: " + playerLife + "\nRECORD: " + scoringRecord);
         
        //Movimentação Inimigo
        for(Spatial s : shootables.getChildren()){
            enemyActions(s, tpf);
        }
        
        if(playerLife <= 0){
            resetVariables();
        }
    }
   
  /** A centred plus sign to help the player aim. */
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
  
  protected Spatial makeCharacter() {

    Spatial enemy = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
    enemy.scale(0.025f);
    enemy.setLocalTranslation(positionEnemy.get(qtdEnemy%6)); 
    qtdEnemy++;
   
    control = enemy.getControl(AnimControl.class);
    channel = control.createChannel();
    channel.setAnim("Walk", 0.50f);
    channel.setLoopMode(LoopMode.Loop);

//Walk
//Kick
//Spin
//Idle3
//Idle2
//Jump
//Idle1
//HighJump
//JumpNoHeight
//Crouch
//Climb
//Stealth
//Death2
//Backflip 
//Block
//SideKick
//Death1
//Attack3
//Attack2
//Attack1
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
  
  public void enemyActions(Spatial s, float tpf){
      s.lookAt(cam.getLocation(),  Vector3f.UNIT_Y.normalize());
            s.rotate(0, (float) Math.PI , 0);
            Vector3f dir = s.getLocalTranslation().subtract(cam.getLocation()).normalize().negate();
            if(cam.getLocation().subtract(s.getLocalTranslation()).getX() < 5 && cam.getLocation().subtract(s.getLocalTranslation()).getX() > -5
               && cam.getLocation().subtract(s.getLocalTranslation()).getZ() < 5 && cam.getLocation().subtract(s.getLocalTranslation()).getZ() > -5){
                    s.move(dir.x*0*tpf, 0, dir.z*0*tpf);  
                    if(!s.getControl(AnimControl.class).getChannel(0).getAnimationName().equals("Attack3") && !s.getControl(AnimControl.class).getChannel(0).getAnimationName().equals("Death1")){
                        s.getControl(AnimControl.class).getChannel(0).setAnim("Attack3");
                        s.getControl(AnimControl.class).getChannel(0).setLoopMode(LoopMode.Cycle);
                    }
                    playerLife -= 1;    
             
                }
            else{
                s.move(dir.x*enemySpeed*tpf, 0, dir.z*enemySpeed*tpf);
                if(!s.getControl(AnimControl.class).getChannel(0).getAnimationName().equals("Walk") && !s.getControl(AnimControl.class).getChannel(0).getAnimationName().equals("Death1")){
                    s.getControl(AnimControl.class).getChannel(0).setAnim("Walk");
                    s.getControl(AnimControl.class).getChannel(0).setLoopMode(LoopMode.Loop);
                }
            }
            
    }
  
  public void resetVariables(){
      enemySpeed = 5f;
      playerLife = 200;
      scoringRecord = score;
      score = 0;
  }
}