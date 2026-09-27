package presentation.game;

/** Face-local UV atlas. Rows: head, torso, trousers, arms; columns: +Z,-Z,+X,-X,+Y,-Y. */
public final class ZombieAtlas {
    public static final int TILE = 8, WIDTH = 48, HEIGHT = 32;
    public static final int FRONT = 0, BACK = 1;
    private ZombieAtlas() { }

    public static int[][] pixels() {
        int[][] pixels = new int[HEIGHT][WIDTH];
        for (int part=0; part<4; part++) for(int face=0;face<6;face++)
            for(int row=0;row<TILE;row++) for(int col=0;col<TILE;col++) {
                int color = part==0 ? ZombieSkin.SKIN : (part==1||part==3) ? ZombieSkin.SHIRT : ZombieSkin.PANTS;
                if(col==0||col==7||row==0) color=part==0?ZombieSkin.SKIN_DARK:(part==1||part==3)?ZombieSkin.SHIRT_DARK:ZombieSkin.PANTS;
                if(part==2&&row>=6) color=ZombieSkin.SHOES;
                if(part==3&&(face==FRONT||(face==2&&col<=1)||(face==3&&col>=6))) color=ZombieSkin.SKIN;
                if(part==0&&face==FRONT) {
                    if(row==3&&(col==1||col==2||col==5||col==6)) color=ZombieSkin.EYE;
                    if(row==6&&col>=2&&col<=5) color=ZombieSkin.MOUTH;
                }
                pixels[part*TILE+row][face*TILE+col]=color;
            }
        return pixels;
    }
    /** Normalized UV: top-origin Pixmap follows LibGDX texture upload/region convention. */
    public static float[] uv(int part, int face) {
        if(part<0||part>3||face<0||face>5) throw new IllegalArgumentException("Invalid body part/face");
        return new float[]{face/6f, part/4f, (face+1)/6f, (part+1)/4f};
    }
}
