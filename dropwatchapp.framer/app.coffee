layers = Framer.Importer.load "imported/drop_watch_app_v1-01"

Framer.Defaults.Animation = curve: "spring(260,30,0,0)"

found_screen = layers['Layer 2']
found_screen_content = layers['found main content']
found_top_bar = layers['found top bar']
background = layers['background']
choose_screen = layers['Layer 3']
choose_screen.opacity = 0
choose_screen_content = layers['choose main content']
choose_top_bar = layers['choose top bar']
message_screen = layers['Layer 4']
message_screen.opacity = 0
message_top_bar = layers['message top bar']
message_top_bar.y = -107

layers['pick up'].on Events.Click, ->
	found_screen_content.animate
		properties: {x: -320}
	choose_screen.animate
		properties: {opacity: 1}
	found_top_bar.animate
		properties: {opacity: 0}

choose_top_bar.states.add
  up: {y:-107}
message_top_bar.states.add
  down: {y:0}
  
  # Place behind statement
choose_top_bar.states.on Events.StateWillSwitch, (stateA, stateB) ->
  if stateB is "up"
    choose_top_bar.placeBehind(message_top_bar)
  else
    message_top_bar.placeBehind(choose_top_bar)

layers['drop02'].on Events.Click, ->
	choose_screen_content.animate
		properties: {x: -320}
	message_screen.animate
		properties: {opacity: 1}
	choose_top_bar.states.next()
	message_top_bar.states.next()