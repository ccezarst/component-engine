package EngineCore.DefaultComponents;

public enum ComponentType { // TO-DO: change this to an interface based thing so it can be extended :/ OR add more types
	PORT_BINDER,
	PORT_MANAGER,
	CONNECTION_MANAGER,
	PROTOCOL_HANDLER,
	HTTP_ROUTER,
	HTTP_ENDPOINT,
	CONNECTOR,
	INDEXED_CONNECTOR,
	MAPPINGS,
	WEBSITE,
	ERP,
	CACHE,
	DB,
	IN_MEMORY_CACHE,
	STORAGE_CACHE,
	USER_INTERFACE,
	CONSOLE_INTERFACE,
	COMMAND_HANDLER,
    OTHER
}
