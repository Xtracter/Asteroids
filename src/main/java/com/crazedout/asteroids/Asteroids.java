package com.crazedout.asteroids;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Fredrik Roos on 2015-10-26.
 */
public class Asteroids extends JPanel implements Runnable {

    List<Sprite> sprites = new ArrayList<Sprite>();
    List<Sprite> sprite_hits = new ArrayList<Sprite>();
    java.util.List<Sprite> explosions = new ArrayList<Sprite>();

    List<Shot> shots = new ArrayList<Shot>();
    List<Shot> shot_hits = new ArrayList<Shot>();

    Point[] stars = new Point[0];
    Rectangle screen;
    Sprite ship,alien,rocket;

    final String    TITLE = "A S T E R O I D S";
    final String    GAME_OVER = "G A M E   O V E R";
    final String    COPY = "CrazedoutSoft 2015";
    final int       LASER_SOUND = 0;
    final int       CRASH_SOUND = 1;
    final int       WARP_SOUND = 2;
    final int       ROCKET_SOUND = 3;
    final int       CLOCKWISE = -1;
    final int       STOP = 0;
    final int       COUNTER_CLOCKWISE = 1;
    final int       BIG_ASTEROID = 0;
    final int       MEDIUM_ASTEROID = 1;
    final int       SMALL_ASTEROID = 2;
    final int       SPACE_ROCK     = 3;

    long            SPEED = 40;
    double          TURN_SPEED = 10;
    int             SHOT_SPEED = 18;
    int             LEVEL = 0;
    int             LEVEL_WAIT = 3500;
    int             LIVES = 3;
    int             SCORE = 0;
    boolean         MARQUEE = true;
    int             NUM_STARS = 80;
    int             ROTATION =  STOP;
    boolean         CRASH = false;
    boolean         ALIEN_CRASH = false;
    boolean         ACCELERATING = false;
    boolean         DECELERATING = false;
    double          xSpeed = 0;
    double          ySpeed = 0;
    double          acceleration = .50;
    double          deceleration = .25;
    double          drag = 1;

    Thread runner;

    int dx,dy;

      String ctr[] = {"U P - K E Y  =  T H R U S T E R S  O N",
            "R I G H T  - K E Y  =  T U R N  R I G H T",
            "L E F T - K E Y  =  T U R N  L E F T",
            "S P A C E  O R  'F'  =  F I R E ",
            "E S C - K E Y  =  E X I T  G a M E"
    };


    public Asteroids() {
        setBackground(Color.BLACK);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                dx = e.getX();
                dy = e.getY();
            }
        });
        sprites.add(spawnAsteroid(MEDIUM_ASTEROID, new double[]{0, 0, 45}));
        sprites.add(spawnAsteroid(MEDIUM_ASTEROID, new double[]{66, 190, 45}));
        sprites.add(spawnAsteroid(SMALL_ASTEROID, new double[]{100, 100, 73}));
        sprites.add(spawnAsteroid(SMALL_ASTEROID, new double[]{160, 160, 140}));
        sprites.add(spawnAsteroid(SMALL_ASTEROID, new double[]{200, 0, 220}));
        sprites.add(spawnAsteroid(BIG_ASTEROID, new double[]{81, 160, 140}));
        sprites.add(spawnAsteroid(BIG_ASTEROID, new double[]{200, 93, 220}));

        ship = new Sprite();
        ship.addPoint(15,0);
        ship.addPoint(-10,-8);
        ship.addPoint(-8,0);
        ship.addPoint(-10,8);

        runner = new Thread(this);
        runner.start();
    }

    void start(int level, boolean respawn) {
        start(level, respawn, LIVES);
    }

    void start(int level, boolean respawn, int lives) {

        LIVES = lives;
        CRASH = false;
        MARQUEE=false;
        stars = getStars(getWidth(),getHeight());

        LEVEL=level;
        ship = new Sprite();
        ship.position = new Point(getWidth() / 2, getHeight() / 2);
        ship.addPoint(15,0);
        ship.addPoint(-10,-8);
        ship.addPoint(-8,0);
        ship.addPoint(-10,8);

        rocket = new Sprite();
        rocket.addPoint(-10,5);
        rocket.addPoint(-25,0);
        rocket.addPoint(-10,-5);

        xSpeed = 0;
        ySpeed = 0;

        if(respawn) {
            sprites.clear();
            sprites.add(spawnAsteroid(0));
            sprites.add(spawnAsteroid(0));
            for(int i = 0; i < LEVEL; i++){
                sprites.add(spawnAsteroid(0));
            }
            SCORE=0;
        }

        screen = new Rectangle();
        screen.x = 0;
        screen.y = 0;
        screen.width = getWidth();
        screen.height = getHeight();

        shots.clear();

        ship.rotation = 180;

        runner = new Thread(this);
        runner.start();
    }

    long timer;
    void tick(){
        if(!MARQUEE && sprites.size()<1){
            MARQUEE=true;
            timer = System.currentTimeMillis();
        }else if(MARQUEE && LEVEL>0 && LIVES >0 && !CRASH){
            if(timer+LEVEL_WAIT<System.currentTimeMillis()){
                start(LEVEL+1,true);
            }
        }

        if(alien==null && !MARQUEE && sprites.size()>0 && LIVES>0 && !CRASH){
            Random rand = new Random();
            if(rand.nextInt(500)==1){
                new Thread(new Runnable(){
                    public void run(){
                        while(alien!=null && !ALIEN_CRASH){
                            play(LASER_SOUND,1);
                            try{
                                Thread.sleep(500);
                            }catch(Exception ex){
                                ex.printStackTrace();
                            }
                        }
                    }
                }).start();
                alien=spawnAlien();
            }
        }

        if(alien!=null && !ALIEN_CRASH){
            Random rand = new Random();
            if(rand.nextInt(150)==1){
                new Thread(new Runnable(){
                    public void run(){
                        int c = LEVEL<4?LEVEL:4;
                        while(c-->0){
                            try{
                                alienFire();
                                Thread.sleep(100);
                            }catch(Exception ex){
                                ex.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        Polygon shipPoly=null;
        if(MARQUEE) {
            drawMarquee(g);
        }else {
            drawStars(g);
            if(!CRASH) {
                shipPoly = drawShip(g);
            }
            drawAsteroids(shipPoly,g);
            drawShots(g);
        }
        if(CRASH) {
            drawCrash(g,ship);
        }
        if(ALIEN_CRASH && alien!=null){
            drawAlienCrash(g, alien);
            if(!ALIEN_CRASH){
                alien = null;
            }
        }
        if(alien!=null && !ALIEN_CRASH){
            drawAlien(g);
        }
        java.util.List<Sprite> rem = new ArrayList<Sprite>();
        for(Sprite e:explosions) {
            drawAsteroidCrash(g, e);
            if(e.kill){
                rem.add(e);
            }
        }
        for(Sprite e:rem) {
            explosions.remove(e);
        }
        drawScoreboard(g);
    }

    // ------------------------- Draw stuff


    void drawAlien(Graphics g){
        alien.position = getEndPoint(alien.position.x,alien.position.y,alien.speed,180);
        Polygon ap = rotatePoly(alien, alien.position, alien.rotation);
        g.setColor(Color.WHITE);
        g.fillPolygon(ap.xpoints, ap.ypoints, ap.npoints);
        g.setColor(Color.WHITE);
        g.drawPolygon(ap.xpoints, ap.ypoints, ap.npoints);
        g.setColor(getBackground());
        g.fillRect(alien.position.x - 4, alien.position.y - 2, 8, 4);
        if(checkAlienHit(ap) || alien.position.x<-50){
            ALIEN_CRASH=true;
        }
    }

    void drawShadowedRect(int x, int y, int width, int height, Graphics g) {
        g.setColor(Color.GRAY);
        g.drawRoundRect(x + 1, y + 1, width, height, 8, 8);
        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, width, height, 8, 8);
    }

    void drawShadowedString(String text, int x, int y, Graphics g){
        g.setColor(Color.GRAY);
        g.drawString(text, x + 1, y + 1);
        g.setColor(Color.WHITE);
        g.drawString(text, x, y);
    }

    void crashAnim(Graphics g, int _x, int _y, int iter){
        g.fillOval(_x - iter, _y - iter, 4, 5);
        g.fillOval(_x, _y - iter, 4, 4);
        g.fillOval(_x + iter, _y - iter,5,4);
        g.fillOval(_x , _y - (iter*2),4,5);
        g.fillOval(_x - (iter/2), _y - iter,3,3);
        g.fillOval(_x + iter, _y - iter, 5, 4);
        g.fillOval(_x + iter, _y + iter, 5, 4);
        g.fillOval(_x - iter, _y + iter, 4, 5);
        g.fillOval(_x, _y + (iter * 2), 5, 4);
    }

    void crashRockAnim(Graphics g, int _x, int _y, int iter){
        g.fillOval(_x - iter, _y - iter, 2, 3);
        g.fillOval(_x, _y - iter, 2, 3);
        g.fillOval(_x + iter, _y - iter,3,4);
        g.fillOval(_x , _y - (iter*2),2,3);
        g.fillOval(_x - (iter/2), _y - iter,2,2);
        g.fillOval(_x + iter, _y - iter, 3, 3);
        g.fillOval(_x + iter, _y + iter, 3, 3);
        g.fillOval(_x - iter, _y + iter, 2, 2);
        g.fillOval(_x, _y + (iter * 2), 3, 4);
    }

    void drawCrash(Graphics g, Sprite s){

        int _x = s.position.x;
        int _y = s.position.y;
        g.setColor(Color.WHITE);

        crashAnim(g, _x, _y, s.iter);
        s.iter++;

        if(s.timer ==0) {
            s.timer = System.currentTimeMillis();
        }
        if(s.timer +LEVEL_WAIT<System.currentTimeMillis()){
            s.timer =0;
            CRASH=false;
            MARQUEE= LIVES <1;
            xSpeed = 0;
            ySpeed = 0;
            ship.position = new Point(getWidth()/2,getHeight()/2);
            s.iter=0;
        }
    }

    void drawAlienCrash(Graphics g, Sprite a){

        int _x = a.position.x;
        int _y = a.position.y;

        g.setColor(Color.WHITE);
        if(a.iter>0){
            drawShadowedString("500",_x-10,_y,g);
        }
        crashAnim(g, _x, _y, a.iter);
        a.iter++;
        if(a.timer ==0) {
            play(CRASH_SOUND,0);
            a.timer = System.currentTimeMillis();
        }
        if(a.timer +LEVEL_WAIT<System.currentTimeMillis()){
            ALIEN_CRASH=false;
            a.timer=0;
            a.iter=0;
        }
    }

    void drawAsteroidCrash(Graphics g, Sprite s){

        int _x = s.position.x;
        int _y = s.position.y;
        g.setColor(Color.WHITE);

        crashRockAnim(g, _x, _y, s.iter);
        s.iter++;

        if(s.timer ==0) {
            s.timer = System.currentTimeMillis();
        }
        if(s.timer +LEVEL_WAIT<System.currentTimeMillis()){
            s.timer =0;
            s.iter=0;
            s.kill=true;
        }
    }

    void drawShots(Graphics g){
        for (Shot s : shots) {
            Point p = getEndPoint(s.x, s.y, SHOT_SPEED, s.angle);
            g.fillOval(p.x - 2, p.y - 2, 4, 4);
            s.x = p.x;
            s.y = p.y;
            if (!checkBounds(new Point(s.x, s.y), -80).equals(new Point(s.x, s.y))) {
                shot_hits.add(s);
            }
        }
        for (Shot s : shot_hits) {
            shots.remove(s);
        }
    }

    int ani = 0;
    Polygon drawShip(Graphics g){

        moveShip(getWidth(),getHeight());
        Polygon p = rotatePoly(ship, ship.position, ship.rotation);
        g.setColor(getBackground());
        g.fillPolygon(p.xpoints, p.ypoints, p.npoints);
        g.setColor(Color.WHITE);
        g.drawPolygon(p.xpoints, p.ypoints, p.npoints);
        if(checkAlienCollision(p) || checkShipCollision(p,null)){
            crash();
        }
        if(ACCELERATING){
            Point pt = new Point(ship.position.x,ship.position.y);
            Polygon r = rotatePoly(rocket, pt, ship.rotation);
            g.drawPolygon(r.xpoints,r.ypoints,r.npoints);
        }
        return p;

    }

    Polygon rotatePoly(Polygon p, Point position, double angle){
        Polygon poly = new Polygon();
        for(int i = 0; i < p.npoints; i++){
            Point pnt = rotateShip(new Point(p.xpoints[i], p.ypoints[i]),angle);
            poly.addPoint(pnt.x + position.x,pnt.y + position.y);
        }
        return poly;
    }

    Point rotateShip(Point p, double angle){

        double sinAng = Math.sin((angle / 180) * Math.PI);
        double cosAng = Math.cos((angle / 180) * Math.PI);
        double dx = p.x;
        double dy = p.y;
        Point pr = new Point();
        pr.x = (int)(dx*cosAng-dy*sinAng);
        pr.y = (int)(dx*sinAng+dy*cosAng);

        return pr;
    }

    public void moveShip(int sWidth, int sHeight) {

        switch(ROTATION) {
            case COUNTER_CLOCKWISE:
                ship.rotation += TURN_SPEED;
                break;
            case CLOCKWISE:
                ship.rotation -= TURN_SPEED;
                break;
        }

        double angle = ship.rotation;
        if(ACCELERATING) {
            xSpeed += acceleration * Math.cos((angle / 180) * Math.PI);
            ySpeed += acceleration * Math.sin((angle / 180) * Math.PI);
        }
        if(DECELERATING)  {
            xSpeed -= deceleration * Math.cos((angle / 180) * Math.PI);
            ySpeed -= deceleration * Math.sin((angle / 180) * Math.PI);
        }

        ship.position.x += xSpeed;
        ship.position.y += ySpeed;
        xSpeed *= drag;
        ySpeed *= drag;

        if(ship.position.x<0)
            ship.position.x += sWidth;
        else if(ship.position.x>sWidth)
            ship.position.x -= sWidth;

        if(ship.position.y<0) {
            ship.position.y += sHeight;
        }
        else if(ship.position.y>sHeight)
            ship.position.y -= sHeight;
    }

    void drawAsteroids(Polygon shipPoly, Graphics g){
        for (Sprite a : sprites) {
            a.position = getEndPoint(a.position.x, a.position.y, a.speed, a.angle);
            Polygon p = rotatePoly(a, a.position, a.getRotationAngle());
            g.setColor(getBackground());
            g.fillPolygon(p.xpoints, p.ypoints, p.npoints);
            g.setColor(Color.WHITE);
            g.drawPolygon(p.xpoints, p.ypoints, p.npoints);
            if (checkAsteroidHit(p,a)) {
                sprite_hits.add(a);
                explosions.add(a);
            }else if(shipPoly!=null && checkShipCollision(p,shipPoly)){
                crash();
            }else if(checkAlienCollision(p)){
                sprite_hits.add(a);
            }
            a.position = checkBounds(a.position, -20);
        }
        removeAndRespawn();
    }

    void drawStars(Graphics g){
        g.setColor(Color.WHITE);
        for(int i = 0; i < stars.length; i++){
            g.drawString(".", stars[i].x, stars[i].y);
        }
    }

    public static void drawCenteredString(String s, int w, int h, Graphics g) {
        try {
            FontMetrics fm = g.getFontMetrics();
            int x = (w - fm.stringWidth(s)) / 2 ;
            int y = (fm.getAscent() + (h - (fm.getAscent() + fm.getDescent())) / 2);
            g.drawString(s, x, y);
        }catch(Exception ex){}
    }

    int n = 0;
    void drawMarquee(Graphics g){

        if(n++%2==0) stars = getStars(getWidth(),getHeight());
        drawStars(g);
        drawAsteroids(null, g);
        screen = getVisibleRect();
        g.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 24));
        g.setColor(Color.GRAY);
        drawCenteredString(TITLE, getWidth() - 4, getHeight() - 74, g);
        g.setColor(Color.WHITE);
        drawCenteredString(TITLE, getWidth(), getHeight() - 70, g);
        g.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 16));
        if(LEVEL==0) {
            drawCenteredString("Press F1 to begin", getWidth(), getHeight(), g);
            int w = g.getFontMetrics().stringWidth(COPY);
            g.drawString(COPY, getWidth() / 2 - w / 2, getHeight() - 30);
            int l = 0;
            w = g.getFontMetrics().stringWidth(ctr[2]);
            for(String s:ctr){
                drawShadowedString(s, getWidth() / 2 - w / 2, (getHeight() / 2) + 70 + (l++ * 30), g);
            }
            drawShadowedRect((getWidth()/2-w/2)-20,(getHeight()/2)+40,w+60,170,g);
        }else if(LIVES >0 && sprites.size()<1){
            g.setColor(Color.GRAY);
            drawCenteredString("LEVEL: " + (LEVEL+1), getWidth()-4, getHeight()-4, g);
            g.setColor(Color.WHITE);
            drawCenteredString("LEVEL: " + (LEVEL+1), getWidth(), getHeight(), g);
        }else if(LIVES <=0){
            g.setColor(Color.GRAY);
            drawCenteredString(GAME_OVER, getWidth() - 4, getHeight() - 4, g);
            g.setColor(Color.WHITE);
            drawCenteredString(GAME_OVER, getWidth(), getHeight(), g);
            int w = g.getFontMetrics().stringWidth(COPY);
            g.drawString(COPY, getWidth() / 2 - w / 2, getHeight() - 30);
        }
    }

    void drawScoreboard(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 16));
        String score = "S C O R E : " + SCORE;
        int w = g.getFontMetrics().stringWidth(score);

        drawShadowedString(score, 10, getHeight() - 20, g);
        drawShadowedRect(4, getHeight() - 40, w + 12, 30, g);

        String level = "L E V E L : " + (LEVEL);
        w = g.getFontMetrics().stringWidth(level);

        drawShadowedString(level, getWidth() - 20 - w, getHeight() - 20, g);
        drawShadowedRect(getWidth() - 24 - w, getHeight() - 40, w + 12, 30, g);

        String astr = "R A D A R  C O U N T : " + (sprites.size());
        w = g.getFontMetrics().stringWidth(astr);
        drawShadowedString(astr, getWidth()-20-w, 22, g);
        drawShadowedRect(getWidth()-24-w, 2, w + 12, 30, g);

        for(int i = 0; i < LIVES; i++){
            Polygon p = rotatePoly(ship,new Point(22+(i*22),30),-90);
            g.drawPolygon(p.xpoints,p.ypoints,p.npoints);
        }

    }

    // -------------------------- Check stuff

    boolean checkShipCollision(Polygon p, Polygon shipPoly){

        if(shipPoly!=null){
            for(int i = 0; i < shipPoly.npoints-1; i++){
                if(p.contains(shipPoly.xpoints[i], shipPoly.ypoints[i])){
                    return true;
                }
            }
        }

        // Check for alien fire collision.
        for(Shot s: shots){
            if((s instanceof AlienShot) && p.contains(s.x,s.y)){
                return true;
            }
        }

        return false;
    }

    boolean checkAlienHit(Polygon p){
        for (Shot s : shots) {
            if (!(s instanceof AlienShot) && p.contains(s.x - 2, s.y - 2)) {
                shot_hits.add(s);
                SCORE += 500;
                return true;
            }
        }
        return false;
    }

    boolean checkAlienCollision(Polygon p){
        if(alien!=null && !ALIEN_CRASH){
            for(int i = 0; i < alien.npoints-1; i++){
                if(p.contains(new Point(alien.xpoints[i]+alien.position.x,
                        alien.ypoints[i]+alien.position.y))){
                    return true;
                }
            }
        }
        return false;
    }

    boolean checkAsteroidHit(Polygon p, Sprite sp) {

        for (Shot s : shots) {
            if (p.contains(s.x - 2, s.y - 2)) {
                shot_hits.add(s);
                SCORE += (sp.type + 1) * 100;
                return true;
            }
            if (!checkBounds(new Point(s.x, s.y), -40).equals(new Point(s.x, s.y))) {
                shot_hits.add(s);
            }
        }

        return false;
    }

    Point checkBounds(Point p, int margin) {

        if(screen!=null && !screen.contains(p.x,p.y)){
            if(p.x<margin){
                return new Point(getWidth()-(margin+20),p.y);
            }else if(p.x>getWidth()-margin){
                return new Point(margin,p.y);
            }else if(p.y<margin){
                return new Point(p.x,getHeight()-(margin+20));
            }else if(p.y>getHeight()-margin){
                return new Point(p.x,margin+20);
            }
        }
        return p;
    }


    void crash(){
        play(CRASH_SOUND,1);
        if(!CRASH) {
            LIVES--;
        }
        CRASH=true;
    }

    public void alienFire(){
        play(WARP_SOUND,1);
        double angle = getAngle(alien.position,ship.position);
        System.out.println(angle);
        AlienShot s = new AlienShot();
        s.x = alien.position.x;
        s.y = alien.position.y;
        s.angle = angle;
        shots.add(s);
    }

    void removeAndRespawn(){
        Random rand = new Random();
        for(Sprite a: sprite_hits){
            sprites.remove(a);
            switch(a.type){
                case BIG_ASTEROID:
                    for(int i = 0; i < 1; i++){
                        sprites.add(spawnAsteroid(SMALL_ASTEROID,
                                new double[]{a.position.x, a.position.y, getRandAngle()}));
                    }
                    sprites.add(spawnAsteroid(MEDIUM_ASTEROID,
                            new double[]{a.position.x, a.position.y, getRandAngle()}));
                    break;
                case MEDIUM_ASTEROID:
                    for (int i = 0; i < 1; i++) {
                        sprites.add(spawnAsteroid(SMALL_ASTEROID,
                                new double[]{a.position.x, a.position.y, getRandAngle()}));
                    }
                    sprites.add(spawnAsteroid(SPACE_ROCK,
                            new double[]{a.position.x, a.position.y, getRandAngle()}));
                    break;
                case SMALL_ASTEROID:
                    for (int i = 0; i < 2; i++) {
                        sprites.add(spawnAsteroid(SPACE_ROCK,
                                new double[]{a.position.x, a.position.y, getRandAngle()}));
                    }
                    break;
            }
        }
        sprite_hits.clear();
    }

    void play(final int sound, final int repeat){
        new Thread(new Runnable(){
            public void run(){
                try{
                    switch(sound){
                        case LASER_SOUND:
                            SoundUtils.laser(repeat);
                            break;
                        case WARP_SOUND:
                            SoundUtils.warp(repeat);
                            break;
                        case CRASH_SOUND:
                            SoundUtils.bang();
                            break;
                        case ROCKET_SOUND:
                            //fSfoundUtils.thruster();
                            break;
                    }
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    // ---------------------------- Handle input
    long fireTimer;
    public void fire(){
        if(fireTimer+100>System.currentTimeMillis()) return;
        fireTimer = System.currentTimeMillis();
        play(WARP_SOUND,1);
        Shot s = new Shot();
        Point sp = new Point();
        s.x = sp.x+ship.position.x;
        s.y = sp.y+ship.position.y;
        s.angle = ship.rotation;
        shots.add(s);
    }

    public void burnThrusters(boolean b){
        ACCELERATING = b;
        if(b){
            play(ROCKET_SOUND,1);
        }
    }

    public void turn(int dir){
        ROTATION = dir;
    }

    // ------------------------------ Get Sprites

    double getRandAngle(){
        Random rand = new Random();
        double a = rand.nextInt(360);
        if(a>=350 && a <=10) a = 15;
        else if(a>=80 && a <= 100) a = 115;
        else if(a>=170 && a <= 190) a = 200;
        else if(a>=260 && a <= 280) a = 290;
        return a;
    }

    Sprite spawnAsteroid(int type){
        return spawnAsteroid(type, getStartPosition());
    }

    int an;
    Sprite spawnAsteroid(int type, double start[]){

        Random rand = new Random();
        Sprite p = new Sprite(new Point((int)start[0],(int)start[1]));
        p.angle = start[2];
        p.rotationSpeed = (rand.nextInt(3)*2)+2;
        p.type=type;

        switch(type) {
            case BIG_ASTEROID:
                p.speed=2;
                if(an++%2==0){
                    p.addPoint(-24,-24);
                    p.addPoint(0,-36);
                    p.addPoint(24,-48);
                    p.addPoint(24,-24);
                    p.addPoint(36,0);
                    p.addPoint(36,24);
                    p.addPoint(12,36);
                    p.addPoint(-12,36);
                    p.addPoint(-24,12);
                }else{
                    p.addPoint(0,-36);
                    p.addPoint(24,-36);
                    p.addPoint(48,-12);
                    p.addPoint(48,24);
                    p.addPoint(24,36);
                    p.addPoint(36,60);
                    p.addPoint(0,60);
                    p.addPoint(-36,48);
                    p.addPoint(-12,36);
                    p.addPoint(-36,12);
                }
                break;
            case MEDIUM_ASTEROID:
                p.speed=4;
                if(an++%2==0){
                    p.addPoint(-12,-12);
                    p.addPoint(12,-24);
                    p.addPoint(24,-12);
                    p.addPoint(24,0);
                    p.addPoint(12,12);
                    p.addPoint(12,24);
                    p.addPoint(-12,24);
                }else{
                    p.addPoint(-12,-24);
                    p.addPoint(12,-24);
                    p.addPoint(24,-24);
                    p.addPoint(36,-12);
                    p.addPoint(24,12);
                    p.addPoint(-12,24);
                    p.addPoint(-24,0);
                }
                break;
            case SMALL_ASTEROID:
                p.speed=4;
                p.addPoint(-12,-12);
                p.addPoint(-1,-21);
                p.addPoint(6,-22);
                p.addPoint(17,-10);
                p.addPoint(18,5);
                p.addPoint(12,8);
                p.addPoint(-3,11);
                p.addPoint(-15,6);
                break;
            case SPACE_ROCK:
                p.speed=4;
                p.addPoint(-10,-5);
                p.addPoint(6,-13);
                p.addPoint(12,-5);
                p.addPoint(12,2);
                p.addPoint(8,10);
                p.addPoint(-2,10);
                break;
        }

        return p;
    }

    Sprite spawnAlien(){
        Random rand = new Random();
        alien = new Sprite(new Point(getWidth()-50,
                rand.nextInt(getHeight()/2)+ getHeight()/4));
        alien.angle=270;
        alien.speed=3;
        alien.speed=5;
        alien.rotation=0;

        alien.addPoint(8,-8);
        alien.addPoint(0,-8);
        alien.addPoint(-8,-8);
        alien.addPoint(-16,0);
        alien.addPoint(-24,8);
        alien.addPoint(24,8);

        return alien;
    }

    int start = 0;

    double[] getStartPosition(){

        Random rand = new Random();
        double _x=100,_y=-50,_angle=90;

        switch(start) {
            case 0:
                _x = rand.nextInt((getWidth() - 100) + 30);
                _y = 100;
                _angle = getRandAngle();
                break;
            case 1:
                _x = -50;
                _y = getHeight()-200;
                _angle = getRandAngle();
                break;
            case 2:
                _x = -50;
                _y =  getHeight()-100;
                _angle = getRandAngle();
                break;
        }
        start=start<3?start=start+1:0;
        return new double[]{_x,_y,_angle};
    }


    Point[] getStars(int xMax,int yMax){

        Point[] stars = new Point[NUM_STARS];
        Random rand = new Random();
        for(int i = 0; i < NUM_STARS; i++){
            int row = rand.nextInt(xMax);
            int col = rand.nextInt(yMax);
            stars[i] = new Point(row,col);
        }

        return stars;
    }

    //--------------------------- Math stuff

    double shiftAngle(double a){
        if(a<0){
            return a+360;
        }else if(a>360){
            return a-360;
        }
        return a;
    }

    protected Point getEndPoint(int x, int y, double length, double angle){
        return new Point((int)(x + length * Math.cos(angle * Math.PI / 180)),
                (int)(y + length * Math.sin(angle * Math.PI / 180)));
    }


    protected double getAngle(Point origin, Point target) {
        double angle = Math.toDegrees(Math.atan2(target.y - origin.y,target.x - origin.x));
        if(angle < 0){
            angle += 360;
        }else if(angle > 360){
            angle -= 360;
        }
        return angle;
    }

    Image getIconImage(){

        Image img=new BufferedImage(32, 32,BufferedImage.TYPE_INT_RGB);
        Polygon p = rotatePoly(ship, new Point(16, 16), -90);
        img.getGraphics().drawPolygon(p.xpoints,p.ypoints,p.npoints);
        return img;
    }

    @Override
    public void run() {
        while(Thread.currentThread()==runner && runner!=null){
            try{
                Thread.sleep(SPEED);
                tick();
                repaint();
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }

    }

    // ------------------------------

    class Shot {
        int x,y;
        double angle;
    }

    class AlienShot extends Shot {}

    public class Sprite extends Polygon {

        Point position;
        double rotation=0;
        double rotationSpeed = 4;
        double angle = 45;
        int speed = 6;
        int type = 0;
        int iter = 0;
        long timer = 0;
        boolean kill = false;

        public Sprite(){
            super();
        }

        public Sprite(Point position){
            super();
            this.position=position;
        }

        public double getRotationAngle(){
            rotation += rotationSpeed;
            return rotation;
        }

    }
    public static void main(String argv[]) throws Exception {

        final Asteroids a = new Asteroids();

        UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());

        Object options[] = {"Fullscreen mode","Windows mode","Cancel"};
        int FULLSCREEN = JOptionPane.showOptionDialog(null,"Choose screen mode",a.TITLE, JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,new ImageIcon(a.getIconImage()),options, options[0]);

        if(FULLSCREEN==2 || FULLSCREEN==-1){
            System.exit(0);
        }

        JFrame f = new JFrame(a.TITLE);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setIconImage(a.getIconImage());
        f.setResizable(false);
        f.setSize(1024, 768);
        f.add("Center", a);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        f.setLocation(screen.width/2-512, screen.height/2-384);

        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if(FULLSCREEN==0) {
            f.setUndecorated(true);
            gd.setFullScreenWindow(f);
            DisplayMode modes[] = gd.getDisplayModes();
            for (DisplayMode dm : modes) {
                if (dm.getWidth() == 1024 && dm.getHeight() == 768) {
                    gd.setDisplayMode(dm);
                    break;
                }
            }
        }

        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Point hotSpot = new Point(0, 0);
            BufferedImage cursorImage = new BufferedImage(1, 1, 3);
            java.awt.Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");
            f.setCursor(invisibleCursor);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        f.setVisible(true);

        f.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F3:
                        a.spawnAlien();
                        break;
                    case KeyEvent.VK_F1:
                        a.start(1, true, 3);
                        break;
                    case KeyEvent.VK_LEFT:
                        a.turn(a.CLOCKWISE);
                        break;
                    case KeyEvent.VK_RIGHT:
                        a.turn(a.COUNTER_CLOCKWISE);
                        break;
                    case KeyEvent.VK_UP:
                        a.burnThrusters(true);
                        break;
                    case KeyEvent.VK_DOWN:
                        a.DECELERATING = true;
                        break;
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_F:
                        a.fire();
                        break;
                    case KeyEvent.VK_F12:
                        a.sprites.clear();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        gd.setFullScreenWindow(null);
                        System.exit(0);
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    a.turn(a.STOP);
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    a.burnThrusters(false);

                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    a.DECELERATING = false;
                }
            }
        });
    }
}
