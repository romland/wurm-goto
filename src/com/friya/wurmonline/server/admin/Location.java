package com.friya.wurmonline.server.admin;

class Location
{
	private String _name;
	private int _x;
	private int _y;
	private int _layer = 0;
	private int _height = -10;

	Location(String name, int x, int y, int layer)
	{
		this(name, x, y, layer, 0);
	}

	Location(String name, int x, int y, int layer, int height)
	{
		_name = name;
		_x = x;
		_y = y;
		_layer = layer;
		_height = height;
	}

	String getName() { return _name; }
	int getX() { return _x; }
	int getY() { return _y; }
	int getH() { return _height; }
	int getLayer() { return _layer; }
	
	void setH(int h) { _height = h; }
}
