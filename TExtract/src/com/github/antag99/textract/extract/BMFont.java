package com.github.antag99.textract.extract;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


class BMFont {
	
	
	/**
	 * Writes a bitmap font, using XNA spritefont data,
	 * to a buffered writer.
	 */
	public static void writeBMFont(String fontName, List<Rectangle> glyphs,
			List<Rectangle> spacing, List<Vector3> kerning, List<Character> characterMap,
			int verticalLineSpacing, float horozontalSpacing, BufferedWriter writer) throws IOException {
		List<String> info = new ArrayList<String>();
		info.add("face=\"" + fontName + "\"");
		info.add("size=-15");
		info.add("bold=0");
		info.add("italic=0");
		info.add("charset=\"\"");
		info.add("unicode=1");
		info.add("strechH=100");
		info.add("smooth=0");
		info.add("aa=2");
		info.add("padding=0,1,1,0");
		info.add("spacing=1,1");
		info.add("outline=0");
		
		List<String> common = new ArrayList<String>();
		common.add("lineHeight=18");
		common.add("base=25");
		common.add("scaleW=256");
		common.add("scaleH=512");
		common.add("pages=1");
		common.add("packed=0");
		common.add("alphaChnl=0");
		common.add("redChnl=4");
		common.add("greenChnl=4");
		common.add("blueChnl=4");
		
		//page id, file
		//chars count
		
		List<List<String>> chars = new ArrayList<List<String>>();
		
		for(int i = 0; i < characterMap.size(); ++i) {
			List<String> char_ = new ArrayList<String>();
			// the id of the character.
			char_.add("id=" + (int)characterMap.get(i));
			
			// the source region of the character
			Rectangle glyph = glyphs.get(i);
			char_.add("x=" + (int)glyph.x);
			char_.add("y=" + (int)glyph.y);
			char_.add("width=" + (int)glyph.width);
			char_.add("height=" + (int)glyph.height);
		
			Rectangle space = spacing.get(i);
			// the offset to apply before rendering the character
			char_.add("xoffset=" + (int)space.x);
			char_.add("yoffset=" + (int)space.y);
			
			// the amount to advance after the character
			char_.add("xadvance=" + (int)space.width);
			// the page id the character image is on
			char_.add("page=0");
			// the channel where the image can be found. 1 = blue, 2 = green, 4 = red, 8 = alpha, 15 = all channels
			char_.add("chnl=15");
			
			chars.add(char_);
		}
		
		writer.write("info");
		for(String i : info) {
			writer.write(' ');
			writer.write(i);
		}
		writer.newLine();
		
		writer.write("common");
		for(String c : common) {
			writer.write(' ');
			writer.write(c);
		}
		writer.newLine();
		
		writer.write("page id=0 file=" + fontName + ".png");
		writer.newLine();
		
		writer.write("chars count=" + chars.size());
		writer.newLine();
		for(List<String> char_ : chars) {
			writer.write("char");
			for(String prop : char_) {
				writer.write(' ');
				writer.write(prop);
			}
			writer.newLine();
		}
		
		// no kerning
		writer.write("kernings count=0");
		writer.newLine();
	}
}
