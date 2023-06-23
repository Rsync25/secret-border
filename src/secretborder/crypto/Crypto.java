package secretborder.crypto;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import secretborder.util.KeyUtil;

public class Crypto {

	private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    public static byte[] generatePrivateKey() {
        try {
        	Security.addProvider(new BouncyCastleProvider());	
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDSA", "BC");
            kpg.initialize(new ECGenParameterSpec("secp256k1"), new SecureRandom());
            KeyPair processorKeyPair = kpg.genKeyPair();
            
            return bytesFromBigInteger(((ECPrivateKey) processorKeyPair.getPrivate()).getS());
        
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] bytesFromBigInteger(BigInteger n) {

        byte[] b = n.toByteArray();

        if (b.length == 32) {
            return b;
        } else if (b.length > 32) {
            return Arrays.copyOfRange(b, b.length - 32, b.length);
        } else {
            byte[] buf = new byte[32];
            System.arraycopy(b, 0, buf, buf.length - b.length, b.length);
            return buf;
        }
    }

    public static byte[] genPubKey(byte[] secKey) {
        BigInteger x = KeyUtil.bigIntFromBytes(secKey);
        if (!(BigInteger.ONE.compareTo(x) <= 0 && x.compareTo(Point.getn().subtract(BigInteger.ONE)) <= 0)) {
            throw new RuntimeException("The secret key must be an integer in the range 1..n-1.");
        }
        Point ret = Point.mul(Point.G, x);
        return Point.bytesFromPoint(ret);
    }

//	public static String encrypt(byte[] input, String password)
//			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
//			InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
//		
//	}
//
//	public static byte[] decrypt(byte[] cipherBytes, String password)
//			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
//			InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
//		
//	}

	public static byte[] encrypt(byte[] input, SecretKey key, IvParameterSpec iv)
			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
			InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		return cipher.doFinal(input);
	}

	public static byte[] decrypt(byte[] cipherBytes, SecretKey key, IvParameterSpec iv)
			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
			InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, key, iv);
		return cipher.doFinal(cipherBytes);
	}

	public static SecretKey getKeyFromPassword(String password, String salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
		SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
		return secret;
	}

	public static IvParameterSpec generateIv() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}

}
