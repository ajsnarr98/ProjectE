package moze_intel.projecte.gameObjs.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.container.ResearchContainer;
import moze_intel.projecte.gameObjs.container.inventory.ResearchInventory;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class GUIResearch extends PEContainerScreen<ResearchContainer> {

    private static final ResourceLocation texture = PECore.rl("textures/gui/research.png");

    private final ResearchInventory inv;

    public GUIResearch(ResearchContainer container, Inventory invPlayer, Component title) {
        super(container, invPlayer, title);
        this.inv = container.researchInventory;
        this.imageWidth = 228;
        this.imageHeight = 196;
        this.titleLabelX = 6;
        this.titleLabelY = 8;
    }

    @Override
    public void init() {
        super.init();

        addRenderableWidget(new Button(leftPos + 13, topPos + 48, 51, 20, PELang.RESEARCH_RESEARCH_BUTTON.translate(), b -> {
            // TODO
            inv.updateClientTargets();
        }));
    }

    @Override
    protected void renderLabels(@NotNull PoseStack matrix, int mouseX, int mouseY) {
        int textColor = 0x404040;
        this.font.draw(matrix, title, titleLabelX, titleLabelY, textColor);

        if (inv.hasItemInfoText()) {
            float xAlign = 75;
            float y = 26;
            float yGap = 14;

            this.font.draw(matrix, inv.getItemResearchFragments(), xAlign, y, textColor);
            y += yGap;
            this.font.draw(matrix, inv.getItemBaseEMC(), xAlign, y, textColor);
            y += yGap;
            this.font.draw(matrix, inv.getItemSellEMC(), xAlign, y, textColor);
            y += yGap;
            this.font.draw(matrix, inv.getItemBuyEMC(), xAlign, y, textColor);
        }
    }

    @Override
    protected void renderBg(@NotNull PoseStack matrix, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
        blit(matrix, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
