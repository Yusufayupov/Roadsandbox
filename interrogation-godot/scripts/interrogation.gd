extends Node3D

const UI_FONT_SIZE := 24
var suspect: Node3D
var torso: Node3D
var head: Node3D
var left_arm: Node3D
var right_arm: Node3D
var eye_l: MeshInstance3D
var eye_r: MeshInstance3D
var dialogue_label: Label
var status_label: Label
var pressure_bar: ProgressBar
var trust_bar: ProgressBar
var pressure := 12.0
var trust := 58.0
var dialogue_index := 0
var action_time := 0.0
var action_kind := ""
var blink_timer := 0.0
var blink_phase := 0.0
var evidence_visible := false
var camera: Camera3D
var materials: Dictionary = {}

const LINES := [
	"Men hech narsani bilmayman... Iltimos, menga ishoning.",
	"Siz aytgan vaqt kamera yozuviga mos kelmayapti.",
	"Mayli... men u yerda edim. Lekin yolg'iz emasdim.",
	"Uning ismi... buni aytishim kerakmi?",
	"Men qo'rqib ketdim. Hammasini boshidan aytib beraman."
]

func _ready() -> void:
	_build_materials()
	_build_room()
	_build_suspect()
	_build_camera()
	_build_ui()
	blink_timer = 2.1
	Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)

func _mat(name: String, color: Color, roughness := 0.78, metallic := 0.0) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = color
	m.roughness = roughness
	m.metallic = metallic
	materials[name] = m
	return m

func _build_materials() -> void:
	_mat("wall", Color(0.105, 0.13, 0.15))
	_mat("wall_light", Color(0.17, 0.20, 0.21))
	_mat("floor", Color(0.075, 0.085, 0.09))
	_mat("metal", Color(0.16, 0.19, 0.20), 0.36, 0.72)
	_mat("wood", Color(0.19, 0.125, 0.085), 0.48)
	_mat("chair", Color(0.055, 0.07, 0.078), 0.92)
	_mat("jacket", Color(0.10, 0.14, 0.17))
	_mat("shirt", Color(0.70, 0.66, 0.59))
	_mat("skin", Color(0.69, 0.43, 0.32), 0.9)
	_mat("skin_light", Color(0.79, 0.54, 0.42), 0.9)
	_mat("hair", Color(0.055, 0.035, 0.033), 0.62)
	_mat("hair_hi", Color(0.12, 0.075, 0.065), 0.58)
	_mat("eye_white", Color(0.88, 0.83, 0.76), 0.42)
	_mat("iris", Color(0.18, 0.105, 0.065), 0.38)
	_mat("pupil", Color(0.012, 0.009, 0.008), 0.22)
	_mat("lip", Color(0.34, 0.12, 0.13), 0.52)
	_mat("gold", Color(0.56, 0.39, 0.19), 0.35, 0.62)
	_mat("paper", Color(0.78, 0.73, 0.61), 0.9)

func _box(parent: Node3D, size: Vector3, pos: Vector3, material: Material, bevel := 0.0) -> MeshInstance3D:
	var mesh := BoxMesh.new()
	mesh.size = size
	var node := MeshInstance3D.new()
	node.mesh = mesh
	node.position = pos
	node.material_override = material
	parent.add_child(node)
	return node

func _sphere(parent: Node3D, radius: float, pos: Vector3, material: Material, scale := Vector3.ONE) -> MeshInstance3D:
	var mesh := SphereMesh.new()
	mesh.radius = radius
	mesh.height = radius * 2.0
	mesh.radial_segments = 32
	mesh.rings = 20
	var node := MeshInstance3D.new()
	node.mesh = mesh
	node.position = pos
	node.scale = scale
	node.material_override = material
	parent.add_child(node)
	return node

func _capsule(parent: Node3D, radius: float, height: float, pos: Vector3, material: Material) -> MeshInstance3D:
	var mesh := CapsuleMesh.new()
	mesh.radius = radius
	mesh.height = height
	mesh.radial_segments = 32
	mesh.rings = 12
	var node := MeshInstance3D.new()
	node.mesh = mesh
	node.position = pos
	node.material_override = material
	parent.add_child(node)
	return node

func _light(parent: Node3D, pos: Vector3, energy: float, color: Color, range_value: float, shadows := false) -> void:
	var light := OmniLight3D.new()
	light.position = pos
	light.light_energy = energy
	light.light_color = color
	light.omni_range = range_value
	light.shadow_enabled = shadows
	parent.add_child(light)

func _build_room() -> void:
	_box(self, Vector3(8.5, 0.2, 8.0), Vector3(0, -0.12, 0), materials["floor"])
	_box(self, Vector3(8.5, 3.8, 0.18), Vector3(0, 1.78, -3.9), materials["wall"])
	_box(self, Vector3(0.18, 3.8, 8.0), Vector3(-4.2, 1.78, 0), materials["wall"])
	_box(self, Vector3(0.18, 3.8, 8.0), Vector3(4.2, 1.78, 0), materials["wall"])
	_box(self, Vector3(8.5, 0.18, 8.0), Vector3(0, 3.68, 0), materials["wall"])
	_box(self, Vector3(2.1, 1.55, 0.07), Vector3(0, 2.0, -3.79), materials["wall_light"])
	_box(self, Vector3(0.035, 1.55, 0.08), Vector3(-1.04, 2.0, -3.73), materials["metal"])
	_box(self, Vector3(0.035, 1.55, 0.08), Vector3(1.04, 2.0, -3.73), materials["metal"])
	# Ceiling fixture and warm pool of light.
	_box(self, Vector3(0.8, 0.08, 0.35), Vector3(0, 3.52, -0.7), materials["metal"])
	_light(self, Vector3(0, 3.25, -0.5), 2.4, Color(1.0, 0.79, 0.57), 5.8, true)
	_light(self, Vector3(-3.0, 2.2, -2.0), 0.65, Color(0.48, 0.62, 0.78), 4.0)
	# Chair frame and upholstery.
	_box(self, Vector3(1.35, 1.65, 0.18), Vector3(0, 0.95, -1.45), materials["chair"])
	_box(self, Vector3(1.4, 0.18, 0.72), Vector3(0, 0.28, -1.05), materials["chair"])
	for x in [-0.55, 0.55]:
		_box(self, Vector3(0.09, 0.65, 0.09), Vector3(x, -0.05, -1.2), materials["metal"])
	# Table between camera and suspect.
	_box(self, Vector3(3.8, 0.14, 1.35), Vector3(0, 0.83, 1.05), materials["wood"])
	for x in [-1.55, 1.55]:
		_box(self, Vector3(0.12, 0.82, 0.12), Vector3(x, 0.38, 1.05), materials["metal"])
	# File folder and paper.
	_box(self, Vector3(0.68, 0.035, 0.48), Vector3(0.15, 0.925, 0.93), materials["gold"])
	_box(self, Vector3(0.48, 0.012, 0.34), Vector3(0.15, 0.95, 0.93), materials["paper"])
	# Wall clock silhouette.
	_sphere(self, 0.27, Vector3(2.9, 2.65, -3.77), materials["metal"], Vector3(1, 1, 0.12))
	_sphere(self, 0.22, Vector3(2.9, 2.65, -3.69), materials["paper"], Vector3(1, 1, 0.12))

func _build_suspect() -> void:
	suspect = Node3D.new()
	suspect.position = Vector3(0, 0, -1.62)
	add_child(suspect)
	# Lower body and torso.
	_capsule(suspect, 0.34, 0.95, Vector3(0, 0.65, 0), materials["jacket"])
	torso = Node3D.new()
	torso.position = Vector3(0, 1.0, 0)
	suspect.add_child(torso)
	_sphere(torso, 0.43, Vector3(0, 0.05, 0), materials["jacket"], Vector3(1.18, 1.28, 0.72))
	_sphere(torso, 0.19, Vector3(0, 0.39, -0.005), materials["shirt"], Vector3(1.0, 0.58, 0.75))
	# Neck and head pivot.
	_capsule(torso, 0.12, 0.34, Vector3(0, 0.62, 0), materials["skin"])
	head = Node3D.new()
	head.position = Vector3(0, 0.92, -0.01)
	torso.add_child(head)
	_sphere(head, 0.31, Vector3(0, 0.02, 0), materials["skin"], Vector3(0.83, 1.13, 0.78))
	# Jaw and cheek volume.
	_sphere(head, 0.22, Vector3(0, -0.12, -0.015), materials["skin_light"], Vector3(0.9, 0.65, 0.78))
	_sphere(head, 0.34, Vector3(0, 0.14, -0.06), materials["hair"], Vector3(1.02, 1.12, 0.92))
	# Hair fringe, side locks.
	_sphere(head, 0.18, Vector3(0, 0.22, 0.205), materials["hair_hi"], Vector3(1.28, 0.48, 0.43))
	for side in [-1.0, 1.0]:
		_capsule(head, 0.075, 0.48, Vector3(side * 0.255, -0.06, -0.02), materials["hair"])
		_sphere(head, 0.055, Vector3(side * 0.27, 0.0, -0.06), materials["skin"], Vector3(0.7, 1.0, 0.6))
	# Eyes, brows, nose, lips are separate geometry for visible expressions.
	for side in [-1.0, 1.0]:
		_sphere(head, 0.067, Vector3(side * 0.115, 0.055, 0.237), materials["eye_white"], Vector3(1.0, 0.52, 0.4))
		var iris := _sphere(head, 0.031, Vector3(side * 0.115, 0.052, 0.264), materials["iris"], Vector3(1, 1, 0.55))
		_sphere(head, 0.016, Vector3(side * 0.115, 0.052, 0.278), materials["pupil"], Vector3(1, 1, 0.5))
		_sphere(head, 0.012, Vector3(side * 0.104, 0.064, 0.286), materials["eye_white"], Vector3(1, 1, 0.5))
		_sphere(head, 0.073, Vector3(side * 0.115, 0.145, 0.22), materials["hair"], Vector3(1.1, 0.22, 0.45))
	_sphere(head, 0.055, Vector3(0, -0.005, 0.29), materials["skin_light"], Vector3(0.62, 0.72, 0.52))
	_sphere(head, 0.085, Vector3(0, -0.19, 0.255), materials["lip"], Vector3(0.85, 0.24, 0.38))
	# Ears.
	for side in [-1.0, 1.0]:
		_sphere(head, 0.075, Vector3(side * 0.255, -0.035, 0), materials["skin"], Vector3(0.45, 0.72, 0.52))
	# Arms resting on table; pivot at shoulders.
	left_arm = Node3D.new()
	left_arm.position = Vector3(-0.34, 0.23, 0.0)
	torso.add_child(left_arm)
	_capsule(left_arm, 0.105, 0.62, Vector3(-0.1, -0.25, -0.22), materials["jacket"])
	_sphere(left_arm, 0.105, Vector3(-0.1, -0.57, -0.34), materials["skin_light"], Vector3(1.0, 0.7, 0.8))
	right_arm = Node3D.new()
	right_arm.position = Vector3(0.34, 0.23, 0.0)
	torso.add_child(right_arm)
	_capsule(right_arm, 0.105, 0.62, Vector3(0.1, -0.25, -0.22), materials["jacket"])
	_sphere(right_arm, 0.105, Vector3(0.1, -0.57, -0.34), materials["skin_light"], Vector3(1.0, 0.7, 0.8))

func _build_camera() -> void:
	camera = Camera3D.new()
	camera.position = Vector3(0, 1.72, 4.75)
	camera.rotation_degrees = Vector3(-3.5, 0, 0)
	camera.fov = 56
	add_child(camera)
	camera.current = true

func _build_ui() -> void:
	var layer := CanvasLayer.new()
	add_child(layer)
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	layer.add_child(root)
	var top := PanelContainer.new()
	top.anchor_left = 0.035
	top.anchor_right = 0.965
	top.anchor_top = 0.018
	top.anchor_bottom = 0.085
	root.add_child(top)
	var top_label := Label.new()
	top_label.text = "INTERROGATION  •  XONA 01                                      21:43"
	top_label.add_theme_font_size_override("font_size", 19)
	top_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	top.add_child(top_label)
	var bottom := PanelContainer.new()
	bottom.anchor_left = 0.025
	bottom.anchor_right = 0.975
	bottom.anchor_top = 0.72
	bottom.anchor_bottom = 0.985
	root.add_child(bottom)
	var column := VBoxContainer.new()
	column.add_theme_constant_override("separation", 8)
	bottom.add_child(column)
	status_label = Label.new()
	status_label.text = "MAHKUMA  •  NODIRA"
	status_label.add_theme_color_override("font_color", Color(0.88, 0.72, 0.49))
	status_label.add_theme_font_size_override("font_size", 17)
	column.add_child(status_label)
	dialogue_label = Label.new()
	dialogue_label.text = LINES[0]
	dialogue_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	dialogue_label.custom_minimum_size.y = 54
	dialogue_label.add_theme_font_size_override("font_size", 20)
	column.add_child(dialogue_label)
	var bars := HBoxContainer.new()
	column.add_child(bars)
	var pbox := VBoxContainer.new()
	pbox.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	bars.add_child(pbox)
	var pl := Label.new()
	pl.text = "BOSIM"
	pbox.add_child(pl)
	pressure_bar = ProgressBar.new()
	pressure_bar.max_value = 100
	pressure_bar.value = pressure
	pressure_bar.show_percentage = false
	pbox.add_child(pressure_bar)
	var tbox := VBoxContainer.new()
	tbox.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	bars.add_child(tbox)
	var tl := Label.new()
	tl.text = "ISHONCH"
	tbox.add_child(tl)
	trust_bar = ProgressBar.new()
	trust_bar.max_value = 100
	trust_bar.value = trust
	trust_bar.show_percentage = false
	tbox.add_child(trust_bar)
	var grid := GridContainer.new()
	grid.columns = 2
	grid.size_flags_vertical = Control.SIZE_EXPAND_FILL
	column.add_child(grid)
	_add_button(grid, "SAVOL BER", _ask)
	_add_button(grid, "DALIL KO'RSAT", _evidence)
	_add_button(grid, "QAT'IY GAPIR", _stern)
	_add_button(grid, "STOLGA UR", _table_hit)

func _add_button(parent: Control, title: String, callback: Callable) -> void:
	var b := Button.new()
	b.text = title
	b.custom_minimum_size = Vector2(0, 54)
	b.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	b.add_theme_font_size_override("font_size", UI_FONT_SIZE)
	b.pressed.connect(callback)
	parent.add_child(b)

func _set_line(index: int) -> void:
	dialogue_index = posmod(index, LINES.size())
	dialogue_label.text = LINES[dialogue_index]
	pressure_bar.value = pressure
	trust_bar.value = trust

func _ask() -> void:
	trust = minf(100.0, trust + 7.0)
	action_kind = "listen"
	action_time = 0.0
	_set_line(dialogue_index + 1)

func _evidence() -> void:
	evidence_visible = not evidence_visible
	pressure = minf(100.0, pressure + 12.0)
	action_kind = "evidence"
	action_time = 0.0
	status_label.text = "DALIL: KAMERA YOZUVI  •  " + ("TOPILDI" if evidence_visible else "YOPILDI")
	_set_line(2 if dialogue_index < 2 else dialogue_index + 1)

func _stern() -> void:
	pressure = minf(100.0, pressure + 17.0)
	action_kind = "stern"
	action_time = 0.0
	head.rotation.x = -0.08
	_set_line(dialogue_index + 1)

func _table_hit() -> void:
	pressure = minf(100.0, pressure + 21.0)
	trust = maxf(0.0, trust - 5.0)
	action_kind = "impact"
	action_time = 0.0
	_set_line(dialogue_index + 1)

func _process(delta: float) -> void:
	var t := Time.get_ticks_msec() / 1000.0
	if is_instance_valid(torso):
		torso.position.y = 1.0 + sin(t * 1.45) * 0.012
	if is_instance_valid(head):
		head.rotation.y = sin(t * 0.48) * 0.035
		head.rotation.x = sin(t * 0.72) * 0.018
		blink_timer -= delta
		if blink_timer <= 0.0:
			blink_phase += delta * 8.0
			var blink := absf(sin(blink_phase))
			var eye_scale := maxf(0.08, 1.0 - blink * 0.92)
			for child in head.get_children():
				if child is MeshInstance3D and child.mesh is SphereMesh and child.position.y > 0.02 and child.position.y < 0.09 and child.position.z < -0.22:
					child.scale.y = eye_scale * 0.52
			if blink_phase > PI:
				blink_timer = randf_range(2.2, 4.4)
				blink_phase = 0.0
	if action_kind == "impact":
		action_time += delta
		var recoil := sin(minf(action_time * 13.0, PI)) * exp(-action_time * 2.8)
		if is_instance_valid(head):
			head.rotation.x = recoil * 0.15
			head.rotation.z = recoil * 0.06
		if is_instance_valid(torso):
			torso.position.z = recoil * 0.035
		if action_time > 0.8:
			action_kind = ""
	elif action_kind == "stern":
		action_time += delta
		if action_time > 0.45:
			action_kind = ""
	# A subtle first-person breathing camera keeps the viewpoint alive.
	camera.position.y = 1.72 + sin(t * 0.75) * 0.006
