class UrlMappings {

	static mappings = {
		"/$pathToFile**"(controller: "image")
		//"404"(view: '/notFound')
		"500"(view:'/error')
	}
}
