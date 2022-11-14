package me.despical.pixelpainter.utils;

import me.despical.pixelpainter.Main;
import me.despical.pixelpainter.utils.image.AsyncImageLoader;
import me.despical.pixelpainter.utils.RGBBlockColor.Pixel;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;

/**
 * @author Despical
 * <p>
 * Created at 14.11.2022
 */
public class SkinCreator {

	public static void log(String s) {
		JavaPlugin.getPlugin(Main.class).getLogger().log(Level.INFO, s);
	}

	public static BufferedImage[] getSkin(String uuid)
		throws NullPointerException, IOException {

		try {
			StringBuilder code = new StringBuilder();
			InputStreamReader is = new InputStreamReader(new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", "")).openStream());

			int charI;

			while ((charI = is.read()) != -1) {
				code.append(((char) charI));
			}

			String[] aaaa = code.toString().split("\"value\" : \"");

			if (aaaa.length == 1) {
				log("The user does not exist- AAAA does not contain value.");
				throw new NullPointerException();
			}

			String decode = new String(decode(aaaa[1].split("\"}],\"legacy\"")[0].split("\"}}}")[0].split("\"")[0]));
			
			String skinURL = decode.split("\"url\" : \"")[1].split("\"")[0];

			BufferedImage[] images = new BufferedImage[2];
			images[0] = ImageIO.read(new URL(skinURL));
			
			if (decode.contains("CAPE")) {
				String capeURL = decode.split("\"url\" : \"")[2].split("\"")[0];
				images[1] = ImageIO.read(new URL(capeURL));
			}

			return images;
		} catch (NullPointerException e) {
			log("The Mojang servers denied the request. Wait a minute or so until you are allowed to get the texture again.");
			throw new NullPointerException();
		} catch (IOException e2) {
			log("The user does not exist.");
			throw new IOException();
		}
	}

	@SuppressWarnings("deprecation")
	public static void createStatue(BufferedImage[] images, Location center,
									Direction dir) {
		BufferedImage skin = images[0];
		BufferedImage cape = images[1];

		Direction front = dir;
		Direction back = null;
		Direction left = null;
		Direction right = null;
		Direction flat = null;

		switch (front) {
			case UP_EAST:
				back = Direction.UP_WEST;
				right = Direction.UP_NORTH;
				left = Direction.UP_SOUTH;
				flat = Direction.FLAT_SOUTHEAST;
				break;
			case UP_WEST:
				back = Direction.UP_EAST;
				right = Direction.UP_SOUTH;
				left = Direction.UP_NORTH;
				flat = Direction.FLAT_NORTHWEST;
				break;
			case UP_NORTH:
				back = Direction.UP_SOUTH;
				right = Direction.UP_EAST;
				left = Direction.UP_WEST;
				flat = Direction.FLAT_SOUTHEAST;
				break;
			case UP_SOUTH:
				back = Direction.UP_NORTH;
				right = Direction.UP_WEST;
				left = Direction.UP_EAST;
				flat = Direction.FLAT_NORTHWEST;
				break;
			default:
				break;
		}

		final Location loc = center.clone();
		{
			a(2,
				0,
				-3,
				loc,
				left,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(0, 32 - 12, 4, 12), 24, false));
			a(-1,
				0,
				0,
				loc,
				right,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(0 + 8, 32 - 12, 4, 12), 24, false));
			a(2,
				0,
				-3,
				loc,
				front,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(0 + 4, 32 - 12, 4, 12), 24, false));
			a(-1, 0, 0, loc, back, front, RGBBlockColor.createResizedCopy(
				skin.getSubimage(0 + 12, 32 - 12, 4, 12), 24, false));
		}

		// Legs (right)
		{
			int x = 16;
			int y = 64;
			if (skin.getHeight() == 32) {
				x = 0;
				y = 32;
			}
			a(2,
				0,
				1,
				loc,
				left,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x, y - 12, 4, 12), 24, false));
			a(-1,
				0,
				4,
				loc,
				right,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x + 8, y - 12, 4, 12), 24, false));
			a(2,
				0,
				1,
				loc,
				front,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x + 4, y - 12, 4, 12), 24, false));
			a(-1,
				0,
				4,
				loc,
				back,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x + 12, y - 12, 4, 12), 24, false));
		}

		// arm (left)
		{
			a(-1,
				23,
				-3 + 8,
				loc,
				flat,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(40 + 4, 16, 4, 4), 8, false));
			a(-1,
				12,
				-3 + 8,
				loc,
				flat,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(40 + 4 + 4, 16, 4, 4), 8, false));

			a(2,
				12,
				-3 + 8,
				loc,
				left,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(40, 20, 4, 12), 24, false));
			a(-1,
				12,
				8,
				loc,
				right,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(40 + 8, 20, 4, 12), 24, false));
			a(2,
				12,
				-3 + 8,
				loc,
				front,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(40 + 4, 20, 4, 12), 24, false));
			a(-1,
				12,
				8,
				loc,
				back,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(40 + 12, 20, 4, 12), 24, false));
		}
		// arm (right)
		{
			int x = 32;
			int y = 48;
			if (skin.getHeight() == 32) {
				x = 32;
				y = 20;
			}
			a(-1,
				23,
				-3 - 4,
				loc,
				flat,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x + 4, y - 4, 4, 4), 8, false));
			a(-1,
				12,
				-3 - 4,
				loc,
				flat,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x + 4 + 4, y - 4, 4, 4), 8, false));
			// Tops and bottoms

			a(2,
				12,
				-3 - 4,
				loc,
				left,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x, y, 4, 12), 24, false));
			a(-1,
				12,
				-4,
				loc,
				right,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x + 8, y, 4, 12), 24, false));
			a(-1,
				12,
				-3 - 4,
				loc,
				front,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x + 4, y, 4, 12), 24, false));
			a(2,
				12,
				-4,
				loc,
				back,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(x + 12, y, 4, 12), 24, false));
		}
		// chest
		{
			a(2,
				12,
				-3,
				loc,
				left,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(16, 20, 4, 12), 24, false));
			a(-1,
				12,
				4,
				loc,
				right,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(16 + 12, 20, 4, 12), 24, false));
			a(2,
				12,
				-3,
				loc,
				front,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(16 + 4, 20, 8, 12), 24, false));
			a(-1,
				12,
				4,
				loc,
				back,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(16 + 16, 20, 8, 12), 24, false));
		}
		// head
		{
			a(-3,
				24,
				-3,
				loc,
				flat,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(16, 0, 8, 8), 16, false));
			a(-3,
				31,
				-3,
				loc,
				flat,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(8, 0, 8, 8), 16, false));

			a(-3,
				24,
				-3,
				loc,
				right,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(0, 8, 8, 8), 16, false));
			a(4,
				24,
				4,
				loc,
				left,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(16, 8, 8, 8), 16, false));
			a(-3,
				24,
				4,
				loc,
				back,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(24, 8, 8, 8), 16, false));
			a(4,
				24,
				-3,
				loc,
				front,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(8, 8, 8, 8), 16, false));
		}
		// helmet
		{
			a(-3,
				23,
				-3,
				loc,
				flat,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(32 + 16, 0, 8, 8), 16, true), true);
			a(-3,
				32,
				-3,
				loc,
				flat,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(32 + 8, 0, 8, 8), 16, true), true);

			a(-3,
				24,
				-4,//-3
				loc,
				right,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(32 + 0, 8, 8, 8), 16, true), true);
			a(4,
				24,
				5,//4,
				loc,
				left,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(32 + 16, 8, 8, 8), 16, true), true);
			a(-4,//-3,
				24,
				4,
				loc,
				back,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(32 + 24, 8, 8, 8), 16, true), true);
			a(5,//4,
				24,
				-3,
				loc,
				front,
				front,
				RGBBlockColor.createResizedCopy(
					skin.getSubimage(32 + 8, 8, 8, 8), 16, true), true);
		}

		if (cape != null) {
			a(-6 + 4,
				6,
				-9 + 4,
				loc,
				front,
				front,
				RGBBlockColor.createResizedCopy(
					cape.getSubimage(0, 0, 12, 18), 18 * 2, true), true);
		}

	}

	private static void a(int x, int y, int z, Location loc, Direction d,
						  Direction f, BufferedImage skin2) {
		a(x, y, z, loc, d, f, skin2, false);
	}

	private static void a(int x, int y, int z, Location loc, Direction d,
						  Direction f, BufferedImage skin2, boolean enableTrans) {

		if (d == Direction.UP_EAST) {
			d = Direction.UP_SOUTH;
		} else if (d == Direction.UP_WEST) {
			d = Direction.UP_NORTH;
		} else if (d == Direction.UP_NORTH) {
			d = Direction.UP_EAST;
			/*
			 * if (f == Direction.UP_EAST || f == Direction.UP_WEST) { d =
			 * Direction.UP_EAST; } else { d = Direction.UP_WEST; }
			 */
		} else if (d == Direction.UP_SOUTH) {
			d = Direction.UP_WEST;
			/*
			 * if (f == Direction.UP_EAST || f == Direction.UP_WEST) { d =
			 * Direction.UP_WEST; } else { d = Direction.UP_EAST; }
			 */
		}
		BufferedImage temp = skin2;
		Pixel[][] result = RGBBlockColor.convertTo2DWithoutUsingGetRGB(temp);
		new AsyncImageLoader(JavaPlugin.getPlugin(Main.class), result, null, getOffset(loc, f, x, y, z), d, temp,
			enableTrans).loadImage(false);
	}

	public static Location getOffset(Location start, Direction d, double xoff,
									 int yoff, double zoff) {
		if (d == Direction.UP_SOUTH) {
			return start.clone().add(-zoff, yoff, -xoff);
		}
		if (d == Direction.UP_NORTH) {
			return start.clone().add(zoff, yoff, xoff);
		}
		if (d == Direction.UP_EAST) {
			return start.clone().add(xoff, yoff, zoff);
		}
		if (d == Direction.UP_WEST) {
			return start.clone().add(-xoff, yoff, -zoff);
		}
		return start.clone().add(xoff, yoff, zoff);
	}


	static private final int  BASELENGTH         = 128;
	static private final int  LOOKUPLENGTH       = 64;
	static private final int  TWENTYFOURBITGROUP = 24;
	static private final int  EIGHTBIT           = 8;
	static private final int  SIXTEENBIT         = 16;
	static private final int  SIXBIT             = 6;
	static private final int  FOURBYTE           = 4;
	static private final int  SIGN               = -128;
	static private final char PAD                = '=';
	static private final boolean fDebug          = false;
	static final private byte [] base64Alphabet        = new byte[BASELENGTH];
	static final private char [] lookUpBase64Alphabet  = new char[LOOKUPLENGTH];

	static {

		for (int i = 0; i < BASELENGTH; ++i) {
			base64Alphabet[i] = -1;
		}
		for (int i = 'Z'; i >= 'A'; i--) {
			base64Alphabet[i] = (byte) (i-'A');
		}
		for (int i = 'z'; i>= 'a'; i--) {
			base64Alphabet[i] = (byte) ( i-'a' + 26);
		}

		for (int i = '9'; i >= '0'; i--) {
			base64Alphabet[i] = (byte) (i-'0' + 52);
		}

		base64Alphabet['+']  = 62;
		base64Alphabet['/']  = 63;

		for (int i = 0; i<=25; i++)
			lookUpBase64Alphabet[i] = (char)('A'+i);

		for (int i = 26,  j = 0; i<=51; i++, j++)
			lookUpBase64Alphabet[i] = (char)('a'+ j);

		for (int i = 52,  j = 0; i<=61; i++, j++)
			lookUpBase64Alphabet[i] = (char)('0' + j);
		lookUpBase64Alphabet[62] = (char)'+';
		lookUpBase64Alphabet[63] = (char)'/';

	}

	protected static boolean isWhiteSpace(char octect) {
		return (octect == 0x20 || octect == 0xd || octect == 0xa || octect == 0x9);
	}

	protected static boolean isPad(char octect) {
		return (octect == PAD);
	}

	protected static boolean isData(char octect) {
		return (octect < BASELENGTH && base64Alphabet[octect] != -1);
	}

	protected static boolean isBase64(char octect) {
		return (isWhiteSpace(octect) || isPad(octect) || isData(octect));
	}

	/**
	 * Encodes hex octects into Base64
	 *
	 * @param binaryData Array containing binaryData
	 * @return Encoded Base64 array
	 */
	public static String encode(byte[] binaryData) {

		if (binaryData == null)
			return null;

		int      lengthDataBits    = binaryData.length*EIGHTBIT;
		if (lengthDataBits == 0) {
			return "";
		}

		int      fewerThan24bits   = lengthDataBits%TWENTYFOURBITGROUP;
		int      numberTriplets    = lengthDataBits/TWENTYFOURBITGROUP;
		int      numberQuartet     = fewerThan24bits != 0 ? numberTriplets+1 : numberTriplets;
		char     encodedData[]     = null;

		encodedData = new char[numberQuartet*4];

		byte k=0, l=0, b1=0,b2=0,b3=0;

		int encodedIndex = 0;
		int dataIndex   = 0;
		if (fDebug) {
			System.out.println("number of triplets = " + numberTriplets );
		}

		for (int i=0; i<numberTriplets; i++) {
			b1 = binaryData[dataIndex++];
			b2 = binaryData[dataIndex++];
			b3 = binaryData[dataIndex++];

			if (fDebug) {
				System.out.println( "b1= " + b1 +", b2= " + b2 + ", b3= " + b3 );
			}

			l  = (byte)(b2 & 0x0f);
			k  = (byte)(b1 & 0x03);

			byte val1 = ((b1 & SIGN)==0)?(byte)(b1>>2):(byte)((b1)>>2^0xc0);

			byte val2 = ((b2 & SIGN)==0)?(byte)(b2>>4):(byte)((b2)>>4^0xf0);
			byte val3 = ((b3 & SIGN)==0)?(byte)(b3>>6):(byte)((b3)>>6^0xfc);

			if (fDebug) {
				System.out.println( "val2 = " + val2 );
				System.out.println( "k4   = " + (k<<4));
				System.out.println( "vak  = " + (val2 | (k<<4)));
			}

			encodedData[encodedIndex++] = lookUpBase64Alphabet[ val1 ];
			encodedData[encodedIndex++] = lookUpBase64Alphabet[ val2 | ( k<<4 )];
			encodedData[encodedIndex++] = lookUpBase64Alphabet[ (l <<2 ) | val3 ];
			encodedData[encodedIndex++] = lookUpBase64Alphabet[ b3 & 0x3f ];
		}

		// form integral number of 6-bit groups
		if (fewerThan24bits == EIGHTBIT) {
			b1 = binaryData[dataIndex];
			k = (byte) ( b1 &0x03 );
			if (fDebug) {
				System.out.println("b1=" + b1);
				System.out.println("b1<<2 = " + (b1>>2) );
			}
			byte val1 = ((b1 & SIGN)==0)?(byte)(b1>>2):(byte)((b1)>>2^0xc0);
			encodedData[encodedIndex++] = lookUpBase64Alphabet[ val1 ];
			encodedData[encodedIndex++] = lookUpBase64Alphabet[ k<<4 ];
			encodedData[encodedIndex++] = PAD;
			encodedData[encodedIndex++] = PAD;
		} else if (fewerThan24bits == SIXTEENBIT) {
			b1 = binaryData[dataIndex];
			b2 = binaryData[dataIndex +1 ];
			l = ( byte ) ( b2 &0x0f );
			k = ( byte ) ( b1 &0x03 );

			byte val1 = ((b1 & SIGN)==0)?(byte)(b1>>2):(byte)((b1)>>2^0xc0);
			byte val2 = ((b2 & SIGN)==0)?(byte)(b2>>4):(byte)((b2)>>4^0xf0);

			encodedData[encodedIndex++] = lookUpBase64Alphabet[ val1 ];
			encodedData[encodedIndex++] = lookUpBase64Alphabet[ val2 | ( k<<4 )];
			encodedData[encodedIndex++] = lookUpBase64Alphabet[ l<<2 ];
			encodedData[encodedIndex++] = PAD;
		}

		return new String(encodedData);
	}

	/**
	 * Decodes Base64 data into octects
	 *
	 * @param encoded string containing Base64 data
	 * @return Array containind decoded data.
	 */
	public static byte[] decode(String encoded) {

		if (encoded == null)
			return null;

		char[] base64Data = encoded.toCharArray();
		// remove white spaces
		int len = removeWhiteSpace(base64Data);

		if (len%FOURBYTE != 0) {
			return null;//should be divisible by four
		}

		int      numberQuadruple    = (len/FOURBYTE );

		if (numberQuadruple == 0)
			return new byte[0];

		byte     decodedData[]      = null;
		byte     b1=0,b2=0,b3=0,b4=0;
		char     d1=0,d2=0,d3=0,d4=0;

		int i = 0;
		int encodedIndex = 0;
		int dataIndex    = 0;
		decodedData      = new byte[ (numberQuadruple)*3];

		for (; i<numberQuadruple-1; i++) {

			if (!isData( (d1 = base64Data[dataIndex++]) )||
				!isData( (d2 = base64Data[dataIndex++]) )||
				!isData( (d3 = base64Data[dataIndex++]) )||
				!isData( (d4 = base64Data[dataIndex++]) ))
				return null;//if found "no data" just return null

			b1 = base64Alphabet[d1];
			b2 = base64Alphabet[d2];
			b3 = base64Alphabet[d3];
			b4 = base64Alphabet[d4];

			decodedData[encodedIndex++] = (byte)(  b1 <<2 | b2>>4 ) ;
			decodedData[encodedIndex++] = (byte)(((b2 & 0xf)<<4 ) |( (b3>>2) & 0xf) );
			decodedData[encodedIndex++] = (byte)( b3<<6 | b4 );
		}

		if (!isData( (d1 = base64Data[dataIndex++]) ) ||
			!isData( (d2 = base64Data[dataIndex++]) )) {
			return null;//if found "no data" just return null
		}

		b1 = base64Alphabet[d1];
		b2 = base64Alphabet[d2];

		d3 = base64Data[dataIndex++];
		d4 = base64Data[dataIndex++];
		if (!isData( (d3 ) ) ||
			!isData( (d4 ) )) {//Check if they are PAD characters
			if (isPad( d3 ) && isPad( d4)) {               //Two PAD e.g. 3c[Pad][Pad]
				if ((b2 & 0xf) != 0)//last 4 bits should be zero
					return null;
				byte[] tmp = new byte[ i*3 + 1 ];
				System.arraycopy( decodedData, 0, tmp, 0, i*3 );
				tmp[encodedIndex]   = (byte)(  b1 <<2 | b2>>4 ) ;
				return tmp;
			} else if (!isPad( d3) && isPad(d4)) {               //One PAD  e.g. 3cQ[Pad]
				b3 = base64Alphabet[ d3 ];
				if ((b3 & 0x3 ) != 0)//last 2 bits should be zero
					return null;
				byte[] tmp = new byte[ i*3 + 2 ];
				System.arraycopy( decodedData, 0, tmp, 0, i*3 );
				tmp[encodedIndex++] = (byte)(  b1 <<2 | b2>>4 );
				tmp[encodedIndex]   = (byte)(((b2 & 0xf)<<4 ) |( (b3>>2) & 0xf) );
				return tmp;
			} else {
				return null;//an error  like "3c[Pad]r", "3cdX", "3cXd", "3cXX" where X is non data
			}
		} else { //No PAD e.g 3cQl
			b3 = base64Alphabet[ d3 ];
			b4 = base64Alphabet[ d4 ];
			decodedData[encodedIndex++] = (byte)(  b1 <<2 | b2>>4 ) ;
			decodedData[encodedIndex++] = (byte)(((b2 & 0xf)<<4 ) |( (b3>>2) & 0xf) );
			decodedData[encodedIndex++] = (byte)( b3<<6 | b4 );

		}

		return decodedData;
	}

	/**
	 * remove WhiteSpace from MIME containing encoded Base64 data.
	 *
	 * @param data  the byte array of base64 data (with WS)
	 * @return      the new length
	 */
	protected static int removeWhiteSpace(char[] data) {
		if (data == null)
			return 0;

		// count characters that's not whitespace
		int newSize = 0;
		int len = data.length;
		for (int i = 0; i < len; i++) {
			if (!isWhiteSpace(data[i]))
				data[newSize++] = data[i];
		}
		return newSize;
	}
}
