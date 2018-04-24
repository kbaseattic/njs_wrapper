module Empty {

	/* @optional foo */
	typedef structure {
		int foo;
	} AType;
	
	/* @id handle */
	typedef string handle_id;
	
	typedef structure {
		handle_id hid;
	} AHandle;

	/* @id ws */
	typedef string ws_ref;

	typedef structure {
		ws_ref ref;
	} ARef;

};