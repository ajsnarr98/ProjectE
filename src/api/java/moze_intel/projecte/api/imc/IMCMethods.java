package moze_intel.projecte.api.imc;

/**
 * This class declares the IMC methods accepted by ProjectE and their argument types
 */
public class IMCMethods {

	/**
	 * This method blacklists an Entity Type from the Swiftwolf Rending Gale's repel effect.
	 *
	 * The Object sent must be an instance of {@link net.minecraft.entity.EntityType}, or else the message is ignored.
	 */
	public static final String BLACKLIST_SWRG = "blacklist_swrg";//TODO - 1.16: Move to tags

	/**
	 * This method blacklists an Entity Type from the Interdiction Torch's repel effect.
	 *
	 * The Object sent must be an instance of {@link net.minecraft.entity.EntityType}, or else the message is ignored.
	 */
	public static final String BLACKLIST_INTERDICTION = "blacklist_interdiction";//TODO - 1.16: Move to tags

	/**
	 * This method blacklists a Tile Entity Type from the Watch of Flowing Time's acceleration.
	 *
	 * The Object sent must be an instance of {@link net.minecraft.tileentity.TileEntityType}, or else the message is ignored.
	 */
	public static final String BLACKLIST_TIMEWATCH = "blacklist_timewatch";//TODO - 1.16: Move to tags

	/**
	 * This method registers a World Transmutation with the Philosopher's Stone.
	 *
	 * The Object sent must be an instance of {@link WorldTransmutationEntry}, or else the message is ignored.
	 */
	public static final String REGISTER_WORLD_TRANSMUTATION = "register_world_transmutation";

	/**
	 * Registers a custom EMC value.
	 *
	 * The Object sent must be an instance of {@link CustomEMCRegistration}, or else the message is ignored.
	 */
	public static final String REGISTER_CUSTOM_EMC = "register_custom_emc";

	/**
	 * Declare a deserializer for a custom {@link moze_intel.projecte.api.nss.NormalizedSimpleStack}
	 *
	 * The Object sent must be an instance of {@link NSSCreatorInfo}, or else the message is ignored.
	 */
	public static final String REGISTER_NSS_SERIALIZER = "register_nss_serializer";
}