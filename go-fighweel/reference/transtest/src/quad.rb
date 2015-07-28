def main
    tri_strips do

        restart_maker 0x7fffffff

        def quad x0,y0, x1,y1, x2,y2, x3, y3
            verts do
                color(1,1,1)

                uv(0, 0)
                vert(x0, y0)

                uv(1, 0)
                vert(x1, y1)

                uv(1, 1)
                vert(x2, y2)

                uv(0, 1)
                vert(x3,y3)

                indicies [0,1,3,2]
            end
        end

        # Axis aligned 2d quad
        def quad_aa x,y,w,h
            x1 = x + w
            y1= y

            x2=  x + w
            y2 = y + h

            x3 = x
            y3 = y + h

            quad(x,y,x1,y1,x2,y2,x3,y3)
        end

        # twonit quad
        def quad_twonit
            quad_aa(-1,-1,2,2)
        end

        def ring width,steps
            raise "steps must be >=2"         unless steps >= 2
            raise "width must be > 0 and <=1" unless (width > 0) && (width <=1)

            my_scale = (1 - width)
            to_rad_mul = (1.0/steps) * (2 * Math::PI)

            # puts "to_rad_mul #{to_rad_mul}"

            (0..(steps-1)).each do |i|

                rad0 = i * to_rad_mul
                rad1 = (i + 1) * to_rad_mul

                # puts "step #{i} rad0 #{rad0} rad1 #{rad1}"

                x0 = Math::cos(rad0)
                y0 = Math::sin(rad0)

                x1 = Math::cos(rad1)
                y1 = Math::sin(rad0)

                x2 = Math::cos(rad1) * my_scale
                y2 = Math::sin(rad1) * my_scale

                x3 = Math::cos(rad0) * my_scale
                y3 = Math::sin(rad1) * my_scale

                quad(x0,y0,
                     x1,y1,
                     x2,y2,
                     x3,y3)
            end
        end

        identity
        ring(0.1,10)

        # amount = 5
        # step = 360 / amount

        # (0..amount).each do |i|

        #     identity
        #     scale(0.5,0.5)
        #     rotate_z(i * step)

        #     quad_twonit
        # end

    end


end

