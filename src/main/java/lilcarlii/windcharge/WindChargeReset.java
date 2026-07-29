package lilcarlii.windcharge;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindChargeReset implements ModInitializer {
	public static final String MOD_ID = "wind-charge-reset";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("wind-charge-reset.yml");
	private static volatile Config config = Config.defaults();

	@Override
	public void onInitialize() {
		reloadConfig();
		registerCommands();
		LOGGER.info("Wind Charge Reset initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static boolean tryResetOnThrow(LivingEntity thrower, Level level) {
		if (!(level instanceof ServerLevel serverLevel) || !(thrower instanceof ServerPlayer player)) {
			return false;
		}

		Config snapshot = config;
		if (!snapshot.enabled) {
			debug(player, snapshot, "blocked: mechanic is disabled");
			return false;
		}

		if (!canReset(player, serverLevel, snapshot)) {
			return false;
		}

		Vec3 resetPosition = player.position();
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(velocity.x, 0.0D, velocity.z);
		player.hurtMarked = true;
		player.resetFallDistance();
		player.fallDistance = 0.0D;

		if (snapshot.sound) {
			serverLevel.playSound(null, resetPosition.x, resetPosition.y, resetPosition.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		if (snapshot.particles) {
			serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, resetPosition.x, resetPosition.y, resetPosition.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}

		debug(player, snapshot, "reset: horizontal velocity kept, vertical velocity set to 0");
		return true;
	}

	private static boolean canReset(ServerPlayer player, ServerLevel level, Config snapshot) {
		if (player.onGround()) {
			debug(player, snapshot, "blocked: player is on the ground");
			return false;
		}

		double verticalVelocity = player.getDeltaMovement().y;
		if (verticalVelocity > -snapshot.minDownwardVelocity) {
			debug(player, snapshot, "blocked: downward velocity " + round(verticalVelocity) + " is slower than " + snapshot.minDownwardVelocity);
			return false;
		}

		float pitch = player.getXRot();
		if (pitch < snapshot.minPitch || pitch > snapshot.maxPitch) {
			debug(player, snapshot, "blocked: pitch " + round(pitch) + " is outside " + snapshot.minPitch + "-" + snapshot.maxPitch);
			return false;
		}

		if (hasSolidGroundWithin(level, player, snapshot.groundBypassDistance)) {
			debug(player, snapshot, "blocked: solid ground is within " + snapshot.groundBypassDistance + " blocks");
			return false;
		}

		return true;
	}

	private static boolean hasSolidGroundWithin(ServerLevel level, ServerPlayer player, double distance) {
		AABB box = player.getBoundingBox();
		double minX = box.minX + 0.001D;
		double maxX = box.maxX - 0.001D;
		double minZ = box.minZ + 0.001D;
		double maxZ = box.maxZ - 0.001D;

		for (double offset = 0.05D; offset <= distance; offset += 0.25D) {
			double y = box.minY - offset;
			if (isSolidGround(level, minX, y, minZ) || isSolidGround(level, minX, y, maxZ) || isSolidGround(level, maxX, y, minZ) || isSolidGround(level, maxX, y, maxZ)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isSolidGround(ServerLevel level, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		BlockState state = level.getBlockState(pos);
		return state.blocksMotion() || state.isFaceSturdy(level, pos, Direction.UP);
	}

	private static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommand(dispatcher));
	}

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("windchargereset")
			.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
			.then(Commands.literal("reload")
				.executes(context -> {
					reloadConfig();
					context.getSource().sendSuccess(() -> Component.literal("Wind Charge Reset config reloaded."), false);
					return 1;
				})));
	}

	private static void reloadConfig() {
		try {
			config = Config.load(CONFIG_PATH);
		} catch (IOException | IllegalArgumentException exception) {
			LOGGER.error("Failed to load {}. Keeping previous config.", CONFIG_PATH, exception);
		}
	}

	private static void debug(ServerPlayer player, Config snapshot, String message) {
		if (snapshot.debug) {
			player.sendSystemMessage(Component.literal("[WindChargeReset] " + message), true);
		}
	}

	private static String round(float value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static String round(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static final class Config {
		private static final String DEFAULT_FILE = """
			# Enable or disable the throw-triggered momentum reset mechanic.
			enabled: true

			# Send trigger/block reasons to the thrower's action bar.
			debug: false

			# Player must be falling at least this fast, in blocks per tick.
			# Example: 0.75 means velocity Y must be -0.75 or lower.
			min-downward-velocity: 0.75

			# Pitch range required to trigger the reset. Minecraft pitch is -90 up, 90 down.
			min-pitch: 75.0
			max-pitch: 90.0

			# Disable resets when solid ground is this close below the player.
			ground-bypass-distance: 2.5

			effects:
			  sound: true
			  particles: true
			""";

		private boolean enabled = true;
		private boolean debug = false;
		private double minDownwardVelocity = 0.75D;
		private double minPitch = 75.0D;
		private double maxPitch = 90.0D;
		private double groundBypassDistance = 2.5D;
		private boolean sound = true;
		private boolean particles = true;

		private static Config defaults() {
			return new Config();
		}

		private static Config load(Path path) throws IOException {
			if (Files.notExists(path)) {
				Files.createDirectories(path.getParent());
				Files.writeString(path, DEFAULT_FILE);
			}

			Config loaded = defaults();
			String section = "";

			for (String rawLine : Files.readAllLines(path)) {
				String line = rawLine.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				if (line.endsWith(":")) {
					section = line.substring(0, line.length() - 1).trim();
					continue;
				}

				int split = line.indexOf(':');
				if (split < 0) {
					continue;
				}

				String key = line.substring(0, split).trim();
				String value = line.substring(split + 1).trim();
				if ("effects".equals(section)) {
					key = "effects." + key;
				}

				loaded.apply(key, value);
			}

			return loaded;
		}

		private void apply(String key, String value) {
			switch (key) {
				case "enabled" -> enabled = parseBoolean(key, value);
				case "debug" -> debug = parseBoolean(key, value);
				case "min-downward-velocity" -> minDownwardVelocity = parseDouble(key, value);
				case "min-pitch" -> minPitch = parseDouble(key, value);
				case "max-pitch" -> maxPitch = parseDouble(key, value);
				case "ground-bypass-distance" -> groundBypassDistance = parseDouble(key, value);
				case "effects.sound" -> sound = parseBoolean(key, value);
				case "effects.particles" -> particles = parseBoolean(key, value);
				default -> {
				}
			}
		}

		private static boolean parseBoolean(String key, String value) {
			if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
				return Boolean.parseBoolean(value);
			}

			throw new IllegalArgumentException("Expected boolean for " + key + ", got " + value);
		}

		private static double parseDouble(String key, String value) {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException exception) {
				throw new IllegalArgumentException("Expected number for " + key + ", got " + value, exception);
			}
		}
	}
}
