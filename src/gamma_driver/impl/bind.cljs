(ns gamma-driver.impl.bind)

(defn bind-dispatch-fn [element data]
  (if (= :variable (:tag element))
    (cond
      (= :attribute (:storage element)) :attribute
      (and
        (= :uniform (:storage element))
        (= :sampler2D (:type element))) :texture-uniform
      (= :uniform (:storage element)) :uniform)
    (cond
      (= :element-index (:tag element)) :element-index
      (= :variable-array (:tag element)) :variable-array)))


(defmulti bind*
          (fn [fns driver program element data]
            (bind-dispatch-fn element data)))


(defmethod bind* :attribute [fns driver program element input]
  (let [{:keys [attribute-input array-buffer]} fns]
    (attribute-input
     driver
     program
     element
     (array-buffer
       driver
       (let [input (if (map? input) input {:data input})
             data (:data input)]
         (assoc input
           :data (if (.-buffer data)
                   data
                   (js/Float32Array. (clj->js (flatten data))))
           :usage :static-draw
           :element element
           :count (if-let [c (:count input)]
                    c
                    (if (vector? data)
                      (count data)))))))))

(defmethod bind* :uniform [fns driver program element input]
  (let [{:keys [uniform-input]} fns]
    (uniform-input
     driver
     program
     element
     (let [input (if (map? input) input {:data input})]
       (assoc input
         :element element
         :data (clj->js (flatten [(:data input)])))))))

(defmethod bind* :element-index [fns driver program element input]
  (let [{:keys [element-array-buffer]} fns]
    (let [spec (let [input (if (map? input)
                            input
                            {:data input})]
                (assoc input
                  ;; Probably already flattened, but keeping it here for now
                  :data (js/Uint16Array. (clj->js (flatten (:data input))))
                  :usage :static-draw
                  :element element
                  :count (count (:data input))))]
     (element-array-buffer driver spec))))


(defmethod bind* :texture-uniform [fns driver program variable input]
  (let [{:keys [texture-uniform-input texture]} fns]
    (texture-uniform-input
     driver
     program
     variable
     (texture
       driver
       ;; not sure if this is the right logic
       input))))


;; program should do useProgram; basic driver should cache the program

;; (.useProgram (:gl driver) (:program program))

(defn bind [fns driver program data]
  (let [p ((:program fns) driver program)]
    (doseq [[k v] data]
      (bind* fns driver p k v))))


;; bind should return the program or the driver?

(comment
  (draw-arrays d (bind xx) {:mode :triangles}))



