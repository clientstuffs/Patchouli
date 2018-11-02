package vazkii.patchouli.client.book.template.component;

import com.google.gson.annotations.SerializedName;

import net.minecraft.client.renderer.GlStateManager;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.template.TemplateComponent;
import vazkii.patchouli.client.book.template.TemplateComponent.VariableHolder;

public class ComponentHeader extends TemplateComponent {

	@VariableHolder
	public String text;
	@VariableHolder @SerializedName("color")
	public String colorStr;
	
	boolean centered = true;
	float scale = 1F;
	
	transient int color;
	
	@Override
	public void build(BookPage page, BookEntry entry, int pageNum) {
		try {
			color = Integer.parseInt(colorStr, 16);
		} catch(NumberFormatException e) {
			color = page.book.headerColor;
		}
		
		if(x == -1)
			x = GuiBook.PAGE_WIDTH / 2;
	}
	
	@Override
	public void render(BookPage page, int mouseX, int mouseY, float pticks) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0);
		GlStateManager.scale(scale, scale, scale);
		
		if(centered)
			page.parent.drawCenteredStringNoShadow(text, 0, 0, color);
		else page.fontRenderer.drawString(text, 0, 0, color);
		GlStateManager.popMatrix();
	}
	
}