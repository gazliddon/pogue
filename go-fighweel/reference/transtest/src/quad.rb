def main
    tri_strips do

        restart_maker 0x7fffffff

        amount = 3
        step = 360 / amount
        identity

        (0..amount).each {|i|

            rotate_z step

            push

            scale 0.5, 0.5
            translate 1,1

            verts do
                color(1,0,0)

                uv(0, 0)
                vert(-1, -1)

                uv(1, 0)
                vert(1, -1)

                uv(1, 1)
                vert(1, 1)

                uv(0, 1)
                vert(-1,  1)

                indicies [0,1,3,2]
            end

            pop
        }

    end
end

