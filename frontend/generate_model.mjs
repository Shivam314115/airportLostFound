import * as THREE from 'three';
import { GLTFExporter } from 'three/examples/jsm/exporters/GLTFExporter.js';
import * as fs from 'fs';

// Build a clean, realistic commercial passenger aircraft model (Boeing/Airbus twin-engine jet style)
const planeGroup = new THREE.Group();
planeGroup.name = 'CommercialPassengerAircraft';

// PBR Materials
const bodyMat = new THREE.MeshStandardMaterial({
  color: 0xfafcff,
  roughness: 0.25,
  metalness: 0.15,
  name: 'AircraftFuselage'
});

const silverMetalMat = new THREE.MeshStandardMaterial({
  color: 0xdde4ea,
  roughness: 0.2,
  metalness: 0.8,
  name: 'SilverMetalTrim'
});

const tealTrimMat = new THREE.MeshStandardMaterial({
  color: 0x087f89,
  roughness: 0.35,
  metalness: 0.2,
  name: 'AirlineTealLivery'
});

const darkEngineMat = new THREE.MeshStandardMaterial({
  color: 0x1a242c,
  roughness: 0.5,
  metalness: 0.6,
  name: 'EngineInterior'
});

const glassMat = new THREE.MeshStandardMaterial({
  color: 0x112233,
  roughness: 0.1,
  metalness: 0.9,
  name: 'CockpitCabinGlass'
});

// 1. Fuselage
// Main cabin cylinder
const cabinGeo = new THREE.CylinderGeometry(0.95, 0.95, 8.5, 32);
cabinGeo.rotateZ(Math.PI / 2);
const cabin = new THREE.Mesh(cabinGeo, bodyMat);
cabin.position.set(0, 0, 0);
planeGroup.add(cabin);

// Aerodynamic Nose cone (Lathe/Sphere profile)
const noseGeo = new THREE.ConeGeometry(0.95, 2.8, 32);
noseGeo.rotateZ(-Math.PI / 2);
const nose = new THREE.Mesh(noseGeo, bodyMat);
nose.position.set(4.25 + 1.4, 0, 0);
// Soften the tip slightly
nose.scale.set(1, 0.9, 0.9);
planeGroup.add(nose);

// Cockpit windshield band
const windshieldGeo = new THREE.CylinderGeometry(0.96, 0.96, 0.6, 32, 1, false, Math.PI * 0.15, Math.PI * 0.7);
windshieldGeo.rotateZ(Math.PI / 2);
const windshield = new THREE.Mesh(windshieldGeo, glassMat);
windshield.position.set(4.3, 0.15, 0);
planeGroup.add(windshield);

// Passenger cabin windows stripe (left and right)
for (let side = -1; side <= 1; side += 2) {
  for (let i = -3.5; i <= 3.2; i += 0.45) {
    const winGeo = new THREE.BoxGeometry(0.18, 0.14, 0.05);
    const windowMesh = new THREE.Mesh(winGeo, glassMat);
    windowMesh.position.set(i, 0.25, side * 0.95);
    planeGroup.add(windowMesh);
  }
}

// Tail cone (tapered rear)
const tailConeGeo = new THREE.CylinderGeometry(0.95, 0.25, 4.0, 32);
tailConeGeo.rotateZ(Math.PI / 2);
const tailCone = new THREE.Mesh(tailConeGeo, bodyMat);
tailCone.position.set(-4.25 - 2.0, 0.15, 0);
planeGroup.add(tailCone);

// 2. Wings (Swept back commercial jet wings)
const wingShape = new THREE.Shape();
wingShape.moveTo(0, 0);
wingShape.lineTo(-2.2, 7.5); // Wingtip rear
wingShape.lineTo(-1.6, 7.7); // Wingtip front
wingShape.lineTo(1.8, 0);   // Wing root front
wingShape.closePath();

const extrudeSettings = {
  depth: 0.12,
  bevelEnabled: true,
  bevelSegments: 2,
  steps: 1,
  bevelSize: 0.04,
  bevelThickness: 0.04
};

const rightWingGeo = new THREE.ExtrudeGeometry(wingShape, extrudeSettings);
rightWingGeo.rotateX(Math.PI / 2);

// Right Wing
const rightWing = new THREE.Mesh(rightWingGeo, bodyMat);
rightWing.position.set(0.6, -0.2, 0);
// Subtle dihedral angle (slight upward tilt)
rightWing.rotation.x = -0.05;
planeGroup.add(rightWing);

// Left Wing
const leftWingGeo = rightWingGeo.clone();
leftWingGeo.scale(1, -1, 1);
const leftWing = new THREE.Mesh(leftWingGeo, bodyMat);
leftWing.position.set(0.6, -0.2, 0);
leftWing.rotation.x = 0.05;
planeGroup.add(leftWing);

// Winglet / Sharklets at wingtips (Teal accent)
const wingletGeo = new THREE.BoxGeometry(0.5, 0.8, 0.06);
const rightWinglet = new THREE.Mesh(wingletGeo, tealTrimMat);
rightWinglet.position.set(-1.8, 0.35, 7.6);
rightWinglet.rotation.z = -0.25;
planeGroup.add(rightWinglet);

const leftWinglet = new THREE.Mesh(wingletGeo, tealTrimMat);
leftWinglet.position.set(-1.8, 0.35, -7.6);
leftWinglet.rotation.z = -0.25;
planeGroup.add(leftWinglet);

// 3. Turbofan Jet Engines (pylon mounted under wings)
function createEngine(side) {
  const engineGroup = new THREE.Group();
  
  // Nacelle outer cowl
  const nacelleGeo = new THREE.CylinderGeometry(0.55, 0.5, 2.2, 32);
  nacelleGeo.rotateZ(Math.PI / 2);
  const nacelle = new THREE.Mesh(nacelleGeo, bodyMat);
  engineGroup.add(nacelle);

  // Engine intake lip (silver metal)
  const lipGeo = new THREE.TorusGeometry(0.53, 0.06, 16, 32);
  lipGeo.rotateY(Math.PI / 2);
  const lip = new THREE.Mesh(lipGeo, silverMetalMat);
  lip.position.set(1.1, 0, 0);
  engineGroup.add(lip);

  // Interior fan cone
  const fanGeo = new THREE.ConeGeometry(0.2, 0.5, 16);
  fanGeo.rotateZ(-Math.PI / 2);
  const fanCone = new THREE.Mesh(fanGeo, silverMetalMat);
  fanCone.position.set(0.7, 0, 0);
  engineGroup.add(fanCone);

  const fanDiscGeo = new THREE.CircleGeometry(0.48, 24);
  fanDiscGeo.rotateY(Math.PI / 2);
  const fanDisc = new THREE.Mesh(fanDiscGeo, darkEngineMat);
  fanDisc.position.set(0.6, 0, 0);
  engineGroup.add(fanDisc);

  // Exhaust nozzle rear
  const exhaustGeo = new THREE.CylinderGeometry(0.38, 0.44, 0.4, 24);
  exhaustGeo.rotateZ(Math.PI / 2);
  const exhaust = new THREE.Mesh(exhaustGeo, silverMetalMat);
  exhaust.position.set(-1.25, 0, 0);
  engineGroup.add(exhaust);

  // Pylon connecting engine to wing
  const pylonGeo = new THREE.BoxGeometry(1.2, 0.45, 0.1);
  const pylon = new THREE.Mesh(pylonGeo, silverMetalMat);
  pylon.position.set(0.1, 0.45, 0);
  engineGroup.add(pylon);

  engineGroup.position.set(0.8, -0.65, side * 2.8);
  return engineGroup;
}

planeGroup.add(createEngine(1));
planeGroup.add(createEngine(-1));

// 4. Tail Section: Vertical Stabilizer (Fin & Rudder) + Horizontal Stabilizers
const finShape = new THREE.Shape();
finShape.moveTo(0, 0);
finShape.lineTo(-1.8, 0);
finShape.lineTo(-2.8, 3.2);
finShape.lineTo(-2.2, 3.2);
finShape.closePath();

const finGeo = new THREE.ExtrudeGeometry(finShape, {
  depth: 0.1,
  bevelEnabled: true,
  bevelSegments: 2,
  steps: 1,
  bevelSize: 0.03,
  bevelThickness: 0.03
});
finGeo.rotateX(Math.PI / 2);
finGeo.rotateY(Math.PI / 2);

const verticalFin = new THREE.Mesh(finGeo, tealTrimMat);
verticalFin.position.set(-5.0, 0.6, 0.05);
planeGroup.add(verticalFin);

// Horizontal Stabilizers
const hStabShape = new THREE.Shape();
hStabShape.moveTo(0, 0);
hStabShape.lineTo(-1.2, 2.6);
hStabShape.lineTo(-0.8, 2.7);
hStabShape.lineTo(0.7, 0);
hStabShape.closePath();

const hStabGeo = new THREE.ExtrudeGeometry(hStabShape, {
  depth: 0.08,
  bevelEnabled: true,
  bevelSegments: 2,
  steps: 1,
  bevelSize: 0.02,
  bevelThickness: 0.02
});
hStabGeo.rotateX(Math.PI / 2);

const rightHStab = new THREE.Mesh(hStabGeo, bodyMat);
rightHStab.position.set(-6.2, 0.5, 0);
rightHStab.rotation.x = -0.05;
planeGroup.add(rightHStab);

const leftHStabGeo = hStabGeo.clone();
leftHStabGeo.scale(1, -1, 1);
const leftHStab = new THREE.Mesh(leftHStabGeo, bodyMat);
leftHStab.position.set(-6.2, 0.5, 0);
leftHStab.rotation.x = 0.05;
planeGroup.add(leftHStab);

// Livery decorative teal stripe along fuselage belly
const stripeGeo = new THREE.CylinderGeometry(0.96, 0.96, 7.5, 32, 1, false, Math.PI * 0.9, Math.PI * 0.2);
stripeGeo.rotateZ(Math.PI / 2);
const bellyStripe = new THREE.Mesh(stripeGeo, tealTrimMat);
bellyStripe.position.set(0, -0.05, 0);
planeGroup.add(bellyStripe);

// Center and normalize bounding
const box = new THREE.Box3().setFromObject(planeGroup);
const center = new THREE.Vector3();
box.getCenter(center);
planeGroup.position.sub(center);

// Export to binary GLTF (.glb)
const exporter = new GLTFExporter();
exporter.parse(
  planeGroup,
  (gltf) => {
    fs.writeFileSync('/home/shivam/airportLostFound/frontend/public/models/airplane.glb', Buffer.from(gltf));
    console.log('Successfully generated public/models/airplane.glb!');
  },
  (error) => {
    console.error('Error exporting GLTF:', error);
  },
  { binary: true }
);
