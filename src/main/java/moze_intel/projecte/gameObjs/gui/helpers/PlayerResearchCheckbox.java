package moze_intel.projecte.gameObjs.gui.helpers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class PlayerResearchCheckbox extends Checkbox {
    protected final PlayerResearchCheckbox.OnPress onPress;
    protected final TooltipProvider tooltipProvider;
    private final Screen screen;

    public PlayerResearchCheckbox(Screen screen, int x, int y, int width, int height, boolean selected, PlayerResearchCheckbox.OnPress onPress, TooltipProvider tooltipProvider) {
        super(x, y, width, height, TextComponent.EMPTY, selected, false);
        this.onPress = onPress;
        this.tooltipProvider = tooltipProvider;
        this.screen = screen;
    }

    @Override
    public void renderButton(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTick) {
        super.renderButton(matrix, mouseX, mouseY, partialTick);
        if (this.isHoveredOrFocused()) {
            this.renderToolTip(matrix, mouseX, mouseY);
        }
    }

    @Override
    public void onPress() {
        super.onPress();
        this.onPress.onPress(this, selected());
    }

    @Override
    public void renderToolTip(@NotNull PoseStack matrix, int mouseX, int mouseY) {
        screen.renderTooltip(matrix, tooltipProvider.getTooltipComponents(this), Optional.empty(), mouseX, mouseY);
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(PlayerResearchCheckbox checkbox, boolean isChecked);
    }

    @OnlyIn(Dist.CLIENT)
    public interface TooltipProvider {
        List<Component> getTooltipComponents(PlayerResearchCheckbox checkbox);

        // TODO also narrate tooltip?
    }
}
