PI = Math::PI
TAU = PI * 2.0

def cos v
    Math::cos v
end

def sin v
    Math::cos v
end

def circ rad
    [cos(rad), sin(rad)]
end

def quad x0,y0, x1,y1, x2,y2, x3, y3, r = 1, g = 1, b = 1, a =1
    verts do
        color(r,g,b,a)

        uv(0, 0)
        vert(x0, y0)

        uv(1, 0)
        vert(x1, y1)

        uv(1, 1)
        vert(x2, y2)

        uv(0, 1)
        vert(x3, y3)

        indicies [0,1,3,2]
    end
end

# Axis aligned 2d quad
def quad_aa x,y,w,h, r = 1, g = 1, b = 1, a =1
    x1 = x + w
    y1= y

    x2=  x + w
    y2 = y + h

    x3 = x
    y3 = y + h

    quad(x,y,x1,y1,x2,y2,x3,y3, r,g,b,a)
end

# twonit quad
def quad_twonit r = 1, g = 1, b = 1, a =1
    quad_aa(-1,-1,2,2,r,g,b,a)
end

def quad_unit    r = 1, g = 1, b = 1, a =1
    quad_aa(-0.5,-0.5,1,1,r,g,b,a)
end

def cube
    push
    # top and bottom 
    rotate_x(180)
    quad_unit 1,0,1,1
    translate(0,0, -0.5)
    rotate_x(180)
    quad_unit 0,1,0,1

    pop; push

    # # left and right
    # rotate_y(90)
    # translate(-0.5,0,0.5)
    # quad_unit
    # rotate_y(180)
    # translate(1,0,0)
    # quad_unit

    pop; push

    # # front and back
    # rotate_x(90)
    # translate( 0,0.5,0.5 )
    # quad_unit
    # rotate_x( 180 )
    # translate( 0,-0.5,0 )
    # quad_unit

    pop
end

def ring width,steps

    raise "steps must be >=2"         unless steps >= 2
    raise "width must be > 0 and <=1" unless (width > 0) && (width <=1)

    my_scale = (1 - width)
    to_rad_mul = (1.0/steps) * TAU

    range = (0..(steps-1)).map {|i| i}

    def next_i i, steps
        (i + 1) % steps
    end

    verts do

        range.each do |i|
            x0, y0 = circ( i * to_rad_mul )
            x1, y1 = circ( next_i(i, steps) * to_rad_mul )

            uv(0, 0); vert(x0, y0)
            uv(1, 0); vert(x1, y1)
            uv(1, 1); vert(x1 * my_scale, y1 * my_scale)
            uv(0, 1); vert(x0 * my_scale, y0 * my_scale)
        end

        indicies range
    end

end
