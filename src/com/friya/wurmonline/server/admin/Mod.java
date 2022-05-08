package com.friya.wurmonline.server.admin;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.gotti.wurmunlimited.modloader.interfaces.PlayerMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.NoSuchVillageException;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;
import com.wurmonline.server.zones.FocusZone;
import com.wurmonline.server.zones.Zones;

/*
 * TODO:
 * v 1.1
 * 		add support for going to zone
 * 		add support for going to a set height (in dirts)		<---- DOES NOT WORK ... YET
 * 		make sure you replace the config file with the one included here
 */
public class Mod implements WurmServerMod, PlayerMessageListener
{
	@Override
	public boolean onPlayerMessage(Communicator com, String msg)
	{
		if(com.getPlayer().getPower() > 0 && msg.startsWith("#goto")) {
			return cmdGoto(com, msg);
		}
		
		return false;
	}

	private boolean cmdGoto(Communicator com, String cmd)
	{
		String[] tokens = translateCommandline(cmd);
		Location loc = null;
		
		if(tokens.length == 1) {
			tell(com, "Syntax:");
			tell(com, "        #goto <x y> or #goto <deed name> or #goto <player name> or #goto <zone name> or #goto <creature/player id>");
			tell(com, "Examples:");
			tell(com, "        #goto 500 500");
			tell(com, "        #goto 500 500 200");
			tell(com, "        #goto \"Friya's Home\"");
			tell(com, "        #goto friya");
			tell(com, "        #goto \"HOTA Zone\"");
			tell(com, "        #goto 1234567890123 (a creature or player id)");
			tell(com, "Notes:");
			tell(com, "        If the deed name is multiple words, use quotes. E.g. #goto \"Friya's Fantastic Fellowship\"");
			tell(com, "        If there is e.g. a deed called Friya and also a player with the sane name, distinguish between them by doing #goto zone Friya, #goto deed Friya or #goto player Friya");
			tell(com, "        If going to coordinate, make sure it is within the map boundaries.");
			tell(com, "        When going to a zone, you will end up in the center of it.");
			tell(com, "        You cannot go to invisible GMs.");

			return true;
		}
		
		if(tokens.length == 4) {
			if(isNumeric(tokens[1]) && isNumeric(tokens[2]) && isNumeric(tokens[3]) ) {
				// #goto x y h
				loc = getCoordinateLocation(tokens[1], tokens[2]);
				loc.setH(Integer.parseInt(tokens[3]));
			}

		} else if(tokens.length == 3) {
			if(isNumeric(tokens[1]) && isNumeric(tokens[2])) {
				// #goto x y
				loc = getCoordinateLocation(tokens[1], tokens[2]);
				
			} else if(tokens[1].equals("deed")) {
				// #goto deed
				loc = getDeedLocationByName(tokens[2]);

			} else if(tokens[1].equals("player")) {
				// #goto player
				loc = getPlayerLocationByName(tokens[2]);

			} else if(tokens[1].equals("zone")) {
				// #goto zone
				loc = getZoneLocationByName(tokens[2]);
			}

		} else if(tokens.length == 2) {
			// #goto <player name>
			loc = getPlayerLocationByName(tokens[1]);

			if(loc == null && isNumeric(tokens[1])) {
				// #goto <creature id>
				loc = getCreatureLocationById(tokens[1]);
			}
			
			if(loc == null) {
				// #goto <deed name>
				loc = getDeedLocationByName(tokens[1]);
			}

			if(loc == null) {
				// #goto <zone name>
				loc = getZoneLocationByName(tokens[1]);
			}
		}

		if(loc == null || gotoTarget(com, loc) == false) {
			tell(com, "No idea how to go there... Typo? Coordinate is outside map? Target is invisible? Not a player? Already teleporting?");
		} else {
			tell(com, "The fabric of space opens and you appear at " + loc.getName() + " ... or so you hope.");
		}

		return true;
	}

	private boolean gotoTarget(Communicator c, Location loc)
	{
		Player p = c.getPlayer();

		p.setTeleportPoints((short)loc.getX(), (short)loc.getY(), loc.getLayer(), 0);

		if (p.startTeleporting()) {
			c.sendTeleport(false);
			p.teleport();

			if(loc.getH() != -10) {
				// this.getMovementScheme().setPosition(this.teleportX, this.teleportY, this.status.getPositionZ(), this.status.getRotation(), this.getLayer());
				//UtilProxy.setMCZ(p.getMovementScheme(), loc.getH());
				p.setPositionZ(loc.getH() / 10f);
			}
			
			return true;
		}   

		return false;
	}
	
	private Location getCreatureLocationById(String id)
	{
		Creature c = Creatures.getInstance().getCreatureOrNull(Long.parseLong(id));

		if(c != null) {
			return new Location(c.getName(), c.getTileX(), c.getTileY(), 0);
		}

		return null;
	}
	
	private Location getCoordinateLocation(String sx, String sy)
	{
		int x = Integer.parseInt(sx);
		int y = Integer.parseInt(sy);

		if(x < 1 || x > Zones.worldTileSizeX || y < 1 || y > Zones.worldTileSizeY) {
			return null;
		}
		
		return new Location(x + "x" + y, x, y, 0);
	}
	
	private Location getPlayerLocationByName(String name)
	{
		Player p = Players.getInstance().getPlayerOrNull(name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase());

		// if target is an invisible GM, disallow going to them (caller will give a generic error)
		if(p == null || (p.getPower() > 0 && p.isVisible() == false)) {
			return null;
		}
		
		return new Location(p.getName(), p.getTileX(), p.getTileY(), p.getLayer());
	}
	
	private Location getDeedLocationByName(String name)
	{
		Village d = null;

		try {
			d = Villages.getVillage(name);
		} catch (NoSuchVillageException e) {
		}
		
		if(d == null) {
			return null;
		}
		
		return new Location(d.getName(), d.getTokenX(), d.getTokenY(), 0);
	}

	private Location getZoneLocationByName(String zoneName)
	{
		int startX;
		int startY;
		int zoneSize;
		
		for(FocusZone z : FocusZone.getAllZones()) {
			if(z.getName().equals(zoneName)) {
				startX = z.getStartX();
				startY = z.getStartY();
				zoneSize = Math.max(z.getEndX() - z.getStartX(), z.getEndY() - z.getStartY());

				return new Location(zoneName, startX + (zoneSize/2), startY + (zoneSize/2), 0);
			}
		}

		return null;
	}
	
	// thieved: https://commons.apache.org/proper/commons-exec/apidocs/src-html/org/apache/commons/exec/CommandLine.html
	private String[] translateCommandline(String toProcess)
	{
		if (toProcess == null || toProcess.length() == 0) {
			return new String[0];
		}
		
		final int normal = 0;
		final int inQuote = 1;
		final int inDoubleQuote = 2;
		int state = normal;
		final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
		final ArrayList<String> result = new ArrayList<String>();
		final StringBuilder current = new StringBuilder();
		boolean lastTokenHasBeenQuoted = false;
		
		while (tok.hasMoreTokens()) {
			String nextTok = tok.nextToken();

			switch (state) {
				case inQuote:
					if ("\'".equals(nextTok)) {
						lastTokenHasBeenQuoted = true;
						state = normal;
					} else {
						current.append(nextTok);
					}
					break;

				case inDoubleQuote:
					if ("\"".equals(nextTok)) {
						lastTokenHasBeenQuoted = true;
						state = normal;
					} else {
						current.append(nextTok);
					}
			        break;
			        
				default:
					if ("\'".equals(nextTok)) {
						state = inQuote;
					} else if ("\"".equals(nextTok)) {
						state = inDoubleQuote;
					} else if (" ".equals(nextTok)) {
						if (lastTokenHasBeenQuoted || current.length() != 0) {
							result.add(current.toString());
							current.setLength(0);
						}
					} else {
						current.append(nextTok);
					}
					lastTokenHasBeenQuoted = false;
					break;
			}
		}
		
		if (lastTokenHasBeenQuoted || current.length() != 0) {
			result.add(current.toString());
		}
		
		if (state == inQuote || state == inDoubleQuote) {
			throw new RuntimeException("unbalanced quotes in " + toProcess);
		}
		
		return result.toArray(new String[result.size()]);
	}

	private boolean isNumeric(String str)  
	{  
		return str.matches("-?\\d+(\\.\\d+)?"); 
	}

	private void tell(Communicator c, String msg)
	{
		c.sendNormalServerMessage(msg);
	}
}
