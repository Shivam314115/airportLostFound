import { useRef, Suspense } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { useGLTF, OrbitControls, ContactShadows } from '@react-three/drei';
import * as THREE from 'three';

// Genuine commercial passenger aircraft loaded from plane.glb
function PlaneModel() {
  const { scene } = useGLTF('/plane.glb');
  const planeRef = useRef<THREE.Group>(null);
  const timeOffset = useRef(Math.random() * 10);

  // Subtle floating motion
  useFrame((state) => {
    if (!planeRef.current) return;
    const t = state.clock.getElapsedTime() + timeOffset.current;
    planeRef.current.position.y = Math.sin(t * 1.5) * 0.04;
  });

  return (
    <group
      ref={planeRef}
      scale={[0.33, 0.33, 0.33]}
      position={[0, 0, 0]}
      rotation={[-0.05, -1.05, 0.12]}
    >
      {/* 
        plane.glb size: width ~9.5, height ~3.67, length ~8.58. Center: [0, 0.57, -0.19].
        Offsetting by center coordinates centers the fuselage and wings on origin.
      */}
      <primitive
        object={scene}
        position={[0, -0.57, 0.19]}
      />
    </group>
  );
}

useGLTF.preload('/plane.glb');

export default function AirplaneCanvas() {
  return (
    <div
      className="airplane-canvas-container"
      aria-label="Airport Lost & Found 3D Commercial Airplane"
    >
      <Canvas
        camera={{ position: [0.15, 0.3, 5.6], fov: 34 }}
        dpr={[1, Math.min(window.devicePixelRatio, 2)]}
        shadows
        gl={{ antialias: true, alpha: true, powerPreference: 'high-performance' }}
      >
        {/* Soft studio & atmospheric lighting */}
        <ambientLight intensity={0.96} />

        {/* Sunlight highlighting fuselage */}
        <directionalLight
          position={[6, 8, 5]}
          intensity={1.55}
          castShadow
          shadow-mapSize-width={1024}
          shadow-mapSize-height={1024}
        />

        {/* Soft sky-blue fill light */}
        <directionalLight position={[-6, 4, 2]} intensity={0.65} color="#e0f4fc" />

        {/* Warm bounce light */}
        <directionalLight position={[0, -3, 3]} intensity={0.3} color="#f472b6" />

        {/* Commercial passenger airplane */}
        <Suspense fallback={null}>
          <PlaneModel />
        </Suspense>

        {/* Soft ground contact shadow */}
        <ContactShadows
          position={[0, -0.85, 0]}
          opacity={0.3}
          scale={6.5}
          blur={2.2}
          far={3.0}
          color="#071f38"
        />

        {/* Smooth mouse and touch interaction with damping and clamped angles */}
        <OrbitControls
          enablePan={false}
          enableZoom={false}
          minDistance={3.6}
          maxDistance={8.0}
          minPolarAngle={Math.PI * 0.15}
          maxPolarAngle={Math.PI * 0.68}
          enableDamping={true}
          dampingFactor={0.06}
          rotateSpeed={0.8}
        />
      </Canvas>
    </div>
  );
}
