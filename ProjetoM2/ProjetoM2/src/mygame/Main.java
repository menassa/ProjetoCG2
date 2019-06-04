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
  private CharacterControl player;
  private Vector3f walkDirection = new Vector3f();
  private Vector3f walkDirectionEnemy = new Vector3f();
  private boolean left = false, right = false, up = false, down = false;

  
 

  //Temporary vectors used on each frame.
  //They here to avoid instanciating new vectors on each frame
  private Vector3f camDir = new Vector3f();
  private Vector3f camLeft = new Vector3f();
  
  private float enemySpeed = 5f;
  
  private float lastEnemyTime = 0f;
  private float difficultyTime = 0f;

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
    //bulletAppState.setDebugEnabled(true);

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
    CollisionShape sceneShape =
            CollisionShapeFactory.createMeshShape(sceneModel);
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
    // < jME3.2
    // player.setGravity(30f);

    // >= jME3.2
    player.setGravity(new Vector3f(0,-70f,0));

    player.setPhysicsLocation(new Vector3f(0, 10, 0));
    


    // We attach the scene and the player to the rootnode and the physics space,
    // to make them appear in the game world.
    shootables = new Node("Shootables");
    rootNode.attachChild(sceneModel);
    
    bulletAppState.getPhysicsSpace().add(landscape);
    bulletAppState.getPhysicsSpace().add(player);
    
    rootNode.attachChild(shootables);
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

  /** We over-write some navigational key mappings here, so we can
   * add physics-controlled walking and jumping: */
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
      //Some methods used for setting gravity related variables were deprecated in
      //the 3.2 version of the engine. Choose the method that matches your version
      //of the engine.
      // < jME3.2
      //if (isPressed) { player.jump();}

      // >= jME3.2
      if (isPressed) { player.jump(new Vector3f(0,20f,0));}
    }else if (binding.equals("Shoot")) {
        // 1. Reset results list.
        CollisionResults results = new CollisionResults();
        // 2. Aim the ray from cam loc to cam direction.
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());

        for(Spatial s : shootables.getChildren()){
            s.collideWith(ray, results);
            if (results.size() > 0){
                shootables.detachChild(s);
//                bulletAppState.getPhysicsSpace().remove(s);
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
        
        if(timer.getTimeInSeconds() > difficultyTime + 5 && enemySpeed <= 20){
            difficultyTime = timer.getTimeInSeconds();
            enemySpeed = enemySpeed + 5;
        }
         
        //Movimentação Inimigo
        for(Spatial s : shootables.getChildren()){
            s.lookAt(cam.getLocation(),  Vector3f.UNIT_Y.normalize());
            s.rotate(0, (float) Math.PI , 0);
            Vector3f dir = s.getLocalTranslation().subtract(cam.getLocation()).normalize().negate();
//                s.move(dir.x*enemySpeed*tpf, 0, dir.z*enemySpeed*tpf);  
////                s.getControl(CharacterControl.class).setPhysicsLocation(s.getLocalTranslation());
            System.out.println(cam.getLocation().subtract(s.getLocalTranslation()).getX());
       if(cam.getLocation().subtract(s.getLocalTranslation()).getX() < 5 && cam.getLocation().subtract(s.getLocalTranslation()).getX() > -5
               && cam.getLocation().subtract(s.getLocalTranslation()).getY() < 5 && cam.getLocation().subtract(s.getLocalTranslation()).getY() > -5){
           s.move(dir.x*0*tpf, 0, dir.z*0*tpf);  
        }
        else
           s.move(dir.x*enemySpeed*tpf, 0, dir.z*enemySpeed*tpf);  
        
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
    enemy.setLocalTranslation(2.0f, 1f, 2f);
   
    
    AnimChannel channel;
    AnimControl control;
   
    control = enemy.getControl(AnimControl.class);
    channel = control.createChannel();
    channel.setAnim("Walk", 0.50f);
    channel.setLoopMode(LoopMode.Loop);
    
    
//    CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
//    CharacterControl enemy_character = new CharacterControl(capsuleShape, 0.01f);
//    enemy_character.setJumpSpeed(20);
//    enemy_character.setFallSpeed(30);

    //Some methods used for setting gravity related variables were deprecated in
    //the 3.2 version of the engine. Choose the method that matches your version
    //of the engine.
    // < jME3.2
    // player.setGravity(a30f);

    // >= jME3.2
//    enemy_character.setGravity(new Vector3f(0,-30f,0));
//
//   enemy_character.setPhysicsLocation(new Vector3f(0, 10, 0));
//    bulletAppState.getPhysicsSpace().add(enemy_character);
//  
//    enemy.addControl(enemy_character);
//    for (String anim : control.getAnimationNames()) {
//    System.out.println(anim);
//}
    

    return enemy;
  }
  
  public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
    if (animName.equals("Attack1")) {
      channel.setAnim("Walk", 0.50f);
      channel.setLoopMode(LoopMode.DontLoop);
      channel.setSpeed(1f);
    }
    
  }

  public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    // unused
  }


}