package com.wurmonline.shared.util;

import com.wurmonline.server.creatures.MovementScheme;

public class UtilProxy 
{
	static public void setMCZ(MovementScheme o, float z)
	{
		o.setZ(z);
	}
}
