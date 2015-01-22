package asclib.util;

import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.Collections;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


/**
 * 有道词典本地词库读取类
 *
 */
public class YoudaoDict {
	private byte[] content = null;
	
	private class Index {
		public String word = "";
		public int offset = 0;
	}
	
	private class Translate {
		public String phonetic = "";
		public String translate = "";
	}

	private Vector<Index> index = new Vector<Index>();
	private Map<String, Integer> lookup = null;
	private Map<String, Translate> translate = null;
	private int startpos = 0;
	private int wordcount = 0;
	
	/**
	 * 从内存读入字典并建立索引
	 * @param bytes 内存数组（本地字典库内容）
	 * @return true for successful, false for error
	 */
	private boolean loadFromMemory(byte[] bytes) {
		if (bytes.length < 9) return false;
		
		if (bytes[0] != (byte)0xff) return false;
		if (bytes[1] != (byte)0xff) return false;
		if (bytes[2] != (byte)0xff) return false;
		if (bytes[3] != (byte)0xff) return false;
		if (bytes[4] != (byte)1) return false;
		
		content = new byte[bytes.length];
		System.arraycopy(bytes, 0, content, 0, bytes.length);
		
		int c1 = ((int)bytes[5]) & 0xff;
		int c2 = ((int)bytes[6]) & 0xff;
		int c3 = ((int)bytes[7]) & 0xff;
		int c4 = ((int)bytes[8]) & 0xff;
		int size = (c1) | (c2 << 8) | (c3 << 16) | (c4 << 24);
		startpos = size + 5;
		
		for (int pos = 9; pos < startpos; ) {
			size = 255 - (((int)content[pos]) & 0xff);
			Index idx = new Index();
			byte [] text = new byte[size];
			System.arraycopy(content, pos + 1, text, 0, size);
			for (int i = 0; i < text.length; i++) {
				text[i] = (byte)((255 - (((int)text[i]) & 0xff)) & 0xff);
			}
			try {
				idx.word = new String(text, "GBK");
			}
			catch (UnsupportedEncodingException e) {
				return false;
			}
			pos += 1 + size;
			c1 = 255 - (((int)content[pos + 0]) & 0xff);
			c2 = 255 - (((int)content[pos + 1]) & 0xff);
			c3 = 255 - (((int)content[pos + 2]) & 0xff);
			c4 = 255 - (((int)content[pos + 3]) & 0xff);
			idx.offset = (c1) | (c2 << 8) | (c3 << 16) | (c4 << 24);
			idx.offset += startpos;
			pos += 4;
			index.add(idx);
		}
		
		lookup = new HashMap<String, Integer>(index.size());
		translate = new HashMap<String, Translate>();
		wordcount = index.size();
		
		for (int i = 0; i < wordcount; i++) {
			Index idx = index.get(i);
			lookup.put(idx.word, idx.offset);
		}
		
		Collections.sort(index, new Comparator<Index>() {
				public int compare(Index x, Index y) {
					return x.word.compareTo(y.word);
				}
		});
	
		return true;
	}
	
	private boolean loadFromStream(InputStream is) throws Throwable {
		byte[] cache = new byte[1024 * 16];
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		while (true) {
			int hr = 0;
			try {
				hr = is.read(cache, 0, cache.length);
			}
			catch (IOException e) {
				cache = null;
				throw e;
			}
			if (hr < 0) break;
			os.write(cache, 0, hr);
		}
		cache = os.toByteArray();
		try {
			os.close();
		}	
		catch (IOException e) {
		}
		return loadFromMemory(cache);
	}
	
	private boolean loadFromFile(String name) throws Throwable {
		FileInputStream fs = null;
		try {
			fs = new FileInputStream(name);
		}	
		catch (IOException e) {
			throw e;
		}
		boolean hr = loadFromStream(fs);
		try {
			fs.close();
		}
		catch (IOException e) {
			throw e;
		}
		return hr;
	}
	
	/**
	 * 从内存读取字典并构造对象
	 * @param bytes 内存数组，dictcn.db/dicten.db 内容
	 * @throws java.lang.Throwable
	 */
	public YoudaoDict(byte[] bytes) throws java.lang.Throwable {
		if (loadFromMemory(bytes) == false) {
			throw new IOException("bad dictionary database");
		}
	}
	
	/**
	 * 从流读入数据并构造对象
	 * @param input 输入流
	 * @throws java.lang.Throwable
	 */
	public YoudaoDict(InputStream input) throws java.lang.Throwable {
		if (loadFromStream(input) == false) {
			throw new IOException("bad dictionary data");
		}
	}
	
	/**
	 * 从文件路径加载数据并构造对象
	 * @param fileName 文件路径
	 * @throws java.lang.Throwable
	 */
	public YoudaoDict(String fileName) throws java.lang.Throwable {
		if (loadFromFile(fileName) == false) {
			throw new IOException("bad dictionary file");
		}
	}
	
	/**
	 * 查询词典并返回 { 读音, 解释 } 的字符串数组
	 * @param word 需要查询的单词
	 * @return 如果存在则返回 { 读音, 解释 }，否则返回 null
	 */
	public String[] get(String word) {
		Integer offset = lookup.get(word);
		if (offset == null) return null;
		Translate t = translate.get(word);
		if (t == null) {
			int pos = offset.intValue();
			int c1 = 255 - (((int)content[pos + 0]) & 0xff);
			int c2 = 255 - (((int)content[pos + 1]) & 0xff);
			int c3 = 255 - (((int)content[pos + 2]) & 0xff);
			int c4 = 255 - (((int)content[pos + 3 + c3]) & 0xff);
			int size = (c2 << 8) | c1;
			byte[] t1 = new byte[c3];
			byte[] t2 = new byte[c4];
			System.arraycopy(content, pos + 3, t1, 0, c3);
			System.arraycopy(content, pos + c3 + 4, t2, 0, c4);
			pos += c3 + 4;
			for (int i = 0; i < c3; i++) {
				t1[i] = (byte)((255 - (((int)t1[i]) & 0xff)) & 0xff);
			}
			for (int i = 0; i < c4; i++) {
				t2[i] = (byte)((255 - (((int)t2[i]) & 0xff)) & 0xff);
			}
			t = new Translate();
			try {
				t.phonetic = new String(t1, "GBK");
				t.translate = new String(t2, "GBK");
			}
			catch (UnsupportedEncodingException e) {
				return null;
			}
			size = size + 0;
			translate.put(word, t);
		}
		return new String[] { t.phonetic, t.translate };
	}
	
	/**
	 * 词典中是否有该单词
	 * @param word 需要查询的单词
	 * @return 包含返回 true，否则返回 false
	 */
	public boolean contains(String word) {
		return lookup.containsKey(word);
	}
	
	/**
	 * 返回字典中有多少个单词
	 * @return 返回字典中的单词个数
	 */
	public int size() {
		return wordcount;
	}
	
	/**
	 * 取得字典中的单词
	 * @param position 单词位置
	 * @return 单词
	 */
	public String key(int position) {
		if (position < 0 || position >= wordcount) return null;
		return index.get(position).word;
	}
	
	/**
	 * 二分匹配前缀，找出前缀相似的一批单词，并返回列表
	 * @param word 被查找
	 * @param count 希望匹配多少个单词
	 * @return 字符串数组，保存着匹配到的单词
	 */
	public String[] match(String word, int count) {
		if (wordcount <= 0) return new String[] {};
		int top = 0;
		int bottom = wordcount - 1;
		int middle = top;
		while (top < bottom) {
			middle = (top + bottom) >> 1;
			if (middle == top || middle == bottom) {
				break;
			}
			String text = index.get(middle).word;
			int cp = word.compareTo(text);
			if (cp == 0) {
				break;
			}
			else if (cp < 0) {
				bottom = middle;
			}
			else {
				top = middle;
			}
		}
		while (word.compareTo(index.get(middle).word) > 0) {
			if (++middle >= wordcount) break;
		}
		if (middle + count > wordcount) {
			count = wordcount - middle;
		}		
		String[] words = new String[count];
		for (int i = 0; i < count; i++) {
			int pos = middle + i;
			words[i] = index.get(pos).word;
		}
		return words;
	}
	
	public static void main(String[] args) throws Throwable {
		// TODO Auto-generated method stub
		//byte c1 = (byte)0xff;
		//System.out.println("result" + String.("a" < "b"));
		YoudaoDict d = new YoudaoDict("e:/english/youdao/dictcn.db");
		System.out.println(" " + ("b".compareTo("b")));
		// 查询单个单词
		String[] t = d.get("apple");
		if (t != null) {
			System.out.printf("%s\n%s\n", t[0], t[1]);
		}	else {
			System.out.printf("not find\n");
		}
		String[] match = d.match("ast", 5);
		if (match != null) {
			for (int i = 0; i < match.length; i++) {
				System.out.printf(">>> %s\n", match[i]);
			}
		}	else {
			System.out.println("error");
		}
	}

}


